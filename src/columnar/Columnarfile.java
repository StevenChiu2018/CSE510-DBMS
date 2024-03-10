package columnar;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
class Columnarfile {
    private int numColumns;
    private ColumnInfo[] columns;
    private Heapfile[] heapfiles;
    private String name;
    private int tupleLength;
    //private int stringSize;

    private Heapfile tidHeap;
    public Columnarfile(String name, ColumnInfo[] columns) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = columns.length;
        this.columns = columns;
        this.name = name;
        this.heapfiles = new Heapfile[numColumns];

        this.tupleLength = 0;
        if(!isFileExist(name+".hdr")){
            //create
            createHeaderFile();
            for(int i = 0; i<numColumns;i++){
                heapfiles[i] = new Heapfile(name+"."+i);
            }
        }else{
            //load
            loadHeaderFile();
        }

        tidHeap = new Heapfile(name+".tid");
        //calculate tuple len
        for(ColumnInfo col:columns){
            this.tupleLength+=col.sizeInBytyes;
        }

    }
    private PageId get_file_entry(String filename) throws HFDiskMgrException {

        PageId tmpId = new PageId();

        try {
            tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
        } catch (Exception e) {
            throw new HFDiskMgrException(e, "Heapfile.java: get_file_entry() failed");
        }

        return tmpId;

    }
    private boolean isFileExist(String name){
        try{
            PageId pageid = get_file_entry(name);
            return pageid!=null&&pageid.pid>0;
        }catch(Exception e){
            return false;
        }
    }

    private void loadHeaderFile() throws HFDiskMgrException, HFException, HFBufMgrException, IOException, InvalidTupleSizeException {
        Heapfile headerfile = new Heapfile(this.name+".hdr");
        Scan sc = headerfile.openScan();
        RID rid = new RID();
        Tuple tuple;
        if((tuple=sc.getNext(rid))!= null){
            byte[] data = tuple.getTupleByteArray();
            numColumns = Convert.getIntValue(0,data);
            this.columns=new ColumnInfo[numColumns];
            int offset = 4;

            for(int i = 0; i<numColumns;i++){

                int nameLen = Convert.getIntValue(offset,data);
                offset+=4;

                String colName = Convert.getStrValue(offset,data,nameLen);
                offset+=nameLen;

                int attrType = Convert.getIntValue(offset,data);
                offset+=4;

                int sizeInBytes = Convert.getIntValue(offset,data);
                offset+=4;

                this.columns[i] = new ColumnInfo(colName,new AttrType(attrType),sizeInBytes);
                this.heapfiles[i] = new Heapfile((this.name+"."+colName));
            }
        }
        sc.closescan();
    }


    private void createHeaderFile() throws IOException, HFDiskMgrException, HFException, HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile hdrf = new Heapfile(this.name+".hdr");

        int size = 4;
        // update size for(length of col name(a number), for name itself, attrtype,sizeInbytes
        for(ColumnInfo col : columns){
            size+=4;
            size+=col.name.getBytes().length;
            size+=4;
            size+=4;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(numColumns);

        for(ColumnInfo col: columns){
            // name, nameByte, AttrByte, size in byte
            byte[] nameByte = col.name.getBytes();
            buffer.putInt(nameByte.length);
            buffer.putInt(col.type.attrType);
            buffer.putInt((col.sizeInBytyes));
        }
        byte[]metaByte = buffer.array();
        hdrf.insertRecord(metaByte);
    }

    private void deleteColumnarFile() throws HFDiskMgrException, InvalidSlotNumberException, InvalidTupleSizeException, HFBufMgrException, FileAlreadyDeletedException, IOException, FileEntryNotFoundException, InvalidPageNumberException, FileIOException, DiskMgrException {
        //delete all columns
        for(int i = 0; i<numColumns;i++){
            this.heapfiles[i].deleteFile();
        }
        // delete header file
        String header = this.name+".hdr";
        SystemDefs.JavabaseDB.delete_file_entry(header);
    }
    public TID insertTuple(byte[] tuplePtr) throws HFDiskMgrException, HFException, HFBufMgrException, IOException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        TID tid = new TID(numColumns);
        // offset for next block..
        int offset = 0;

        for(int i = 0; i < numColumns; i++){
            byte[] colData;
            switch(columns[i].type.attrType){
                case AttrType.attrInteger:
                    if(offset+4<=tuplePtr.length){ // make sure len is within range
                        colData = Arrays.copyOfRange(tuplePtr,offset,offset+4);
                        offset+=4;

                    }else{
                        throw new InvalidTupleSizeException();
                    }
                    break;
                case AttrType.attrString:
                    if(offset+columns[i].sizeInBytyes<=tuplePtr.length){
                        colData=Arrays.copyOfRange(tuplePtr,offset,offset+columns[i].sizeInBytyes);
                        offset+=columns[i].sizeInBytyes;

                    }else{
                        throw new InvalidTupleSizeException();
                    }
                default:
                    throw new UnsupportedOperationException();
            }

            RID newrid = heapfiles[i].insertRecord(colData);
            tid.recordIDs[i] = newrid;
        }

        ByteBuffer tidBuffer = ByteBuffer.allocate(4+(8*numColumns));
        tidBuffer.putInt(numColumns); //save num of cols
        for(RID rid:tid.recordIDs){
            tidBuffer.putInt(rid.pageNo.pid); //save pgnum of each rid
            tidBuffer.putInt(rid.slotNo);//
        }

        byte[] tidData = tidBuffer.array();
        tidHeap.insertRecord(tidData);
        return tid;
    }

}
