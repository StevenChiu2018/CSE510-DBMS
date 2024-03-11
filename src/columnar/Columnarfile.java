package columnar;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
class Columnarfile {
    public int numColumns;
    public String name;
    public int tupleLength;
    public int tidPositionCount = 0;

    public ColumnInfo[] columnsInfo;
    public Heapfile[] columns;
    public Heapfile tidHeap;
    public Heapfile columnInfoHeap;

    public TID[] tids;


    public Columnarfile(String name, ColumnInfo[] columnsInfo) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = columnsInfo.length;
        this.columnsInfo = columnsInfo;
        this.name = name;
        this.columns = new Heapfile[numColumns];
        this.tupleLength = 0;
        
        if(!isFileExist(name)){
            createHeaderFile();
            createColumnInfoHeapfile();
        }else{
            loadHeaderFile();
        }
        this.tidHeap = new Heapfile(name+"-TIDs");
        for(int i = 0; i<numColumns; i++){
            columnsInfo[i].columnNo = i;
            columnsInfo[i].fileName = name+"-"+columnsInfo[i].columnName;

            this.tupleLength += columnsInfo[i].sizeInBytes;
            columns[i] = new Heapfile(columnsInfo[i].fileName);
        }

        
        
    }
    public PageId get_file_entry(String filename) throws HFDiskMgrException {

        PageId tmpId = new PageId();

        try {
            tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
        } catch (Exception e) {
            throw new HFDiskMgrException(e, "Heapfile.java: get_file_entry() failed");
        }

        return tmpId;

    }
    public boolean isFileExist(String name){
        try{
            PageId pageid = get_file_entry(name);
            return pageid!=null&&pageid.pid>0;
        }catch(Exception e){
            return false;
        }
    }

    public void loadHeaderFile() throws HFDiskMgrException, HFException, HFBufMgrException, IOException, InvalidTupleSizeException {
        Heapfile headerfile = new Heapfile(this.name+".-column-info");
        Scan sc = headerfile.openScan();
        RID rid = new RID();
        Tuple tuple;

        if((tuple=sc.getNext(rid))!= null){
            byte[] data = tuple.getTupleByteArray();
            int offset = 0;
            //read num of cols.
            numColumns = Convert.getIntValue(offset,data);
            offset+=Integer.BYTES;

            this.columnsInfo = new ColumnInfo[numColumns];
            this.columns = new Heapfile[numColumns];

            for(int i  = 0; i <numColumns; i++){
                int len = Convert.getIntValue(offset,data); // column name length
                offset+=Integer.BYTES;

                String colName = Convert.getStrValue(offset,data,len);
                offset+=len;

                int attr = Convert.getIntValue(offset,data);
                offset+=Integer.BYTES;

                int sizeInBytes = Convert.getIntValue(offset,data);
                offset+=Integer.BYTES;

                this.columnsInfo[i] = new ColumnInfo(colName,new AttrType(attr),sizeInBytes,i,this.name);
                this.columns[i] = new Heapfile(this.name+"."+colName);
            }

            tidPositionCount = Convert.getIntValue(offset,data);
            offset+=Integer.BYTES;

            // load TID
            loadTIDs();

        }
        sc.closescan();
    }

    public void loadTIDs() throws InvalidTupleSizeException, IOException {
        Scan sc = tidHeap.openScan();
        RID rid = new RID();
        Tuple tp ;

        ArrayList<TID> tList = new ArrayList<>();
        while((tp = sc.getNext(rid))!= null){
            byte[] data = tp.getTupleByteArray();
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

            int numrid = dis.readInt();
            int pos = dis.readInt();

            RID[] recID = new RID[numrid];
            for(int i = 0; i< numrid;i++){
                int pno = dis.readInt();
                int slotno = dis.readInt();
                PageId pageno = new PageId(pno);
                recID[i] = new RID(pageno,slotno);
            }

            TID tid = new TID(numrid,pos,recID);
            tList.add(tid);
        }

        sc.closescan();
        tids = tList.toArray(new TID[0]);
    }


    public void createHeaderFile() throws IOException, HFDiskMgrException, HFException, HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile hdrf = new Heapfile(this.name+"-column-info"); //

        int size = 4;
        // update size for(length of col name(a number), for name itself, attrtype,sizeInbytes
        for(ColumnInfo col : columnsInfo){
            size+=4;
            size+=col.columnName.getBytes().length;
            size+=4;
            size+=4;
        }
        size+=4; // for tid

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(numColumns);

        for(ColumnInfo col: columnsInfo){
            // name, nameByte, AttrByte, size in byte
            byte[] nameByte = col.columnName.getBytes();
            buffer.putInt(nameByte.length);
            buffer.put(nameByte);
            buffer.putInt(col.type.attrType);
            buffer.putInt((col.sizeInBytes));
        }
        buffer.putInt(tidPositionCount);
        byte[]metaByte = buffer.array();
        hdrf.insertRecord(metaByte);
    }

    // serialize col Info into byte array and store rec in heapfile
    public void createColumnInfoHeapfile() throws HFDiskMgrException, HFException, HFBufMgrException, IOException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.columnInfoHeap = new Heapfile(this.name + "-column-info");
        for(ColumnInfo col : columnsInfo){
            byte[] buffer = new byte[col.calculateSpace()];
            col.writeToByteArray(buffer,0);
            this.columnInfoHeap.insertRecord(buffer);
        }
    }

    public void deleteColumnarFile() throws HFDiskMgrException, InvalidSlotNumberException, InvalidTupleSizeException, HFBufMgrException, FileAlreadyDeletedException, IOException, FileEntryNotFoundException, InvalidPageNumberException, FileIOException, DiskMgrException {
        //delete all columns
        for(int i = 0; i<numColumns;i++){
            this.columns[i].deleteFile();
        }
    }
    public TID insertTuple(byte[] tuplePtr) throws HFDiskMgrException, HFException, HFBufMgrException, IOException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        TID tid = new TID(numColumns);
        // offset for next block..
        int offset = 0;

        for(int i = 0; i < numColumns; i++){
            byte[] colData;
            switch(columnsInfo[i].type.attrType){
                case AttrType.attrInteger:
                    if(offset+4<=tuplePtr.length){ // make sure len is within range
                        colData = Arrays.copyOfRange(tuplePtr,offset,offset+4);
                        offset+=4;

                    }else{
                        throw new InvalidTupleSizeException();
                    }
                    break;
                case AttrType.attrString:
                    if(offset+columnsInfo[i].sizeInBytes<=tuplePtr.length){
                        colData=Arrays.copyOfRange(tuplePtr,offset,offset+columnsInfo[i].sizeInBytes);
                        offset+=columnsInfo[i].sizeInBytes;

                    }else{
                        throw new InvalidTupleSizeException();
                    }
                default:
                    throw new UnsupportedOperationException();
            }

            RID newrid = columns[i].insertRecord(colData);
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
        tidPositionCount++;
        tid.setPosition(tidPositionCount);
        return tid;
    }

}
