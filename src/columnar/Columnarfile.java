package columnar;
import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
class Columnarfile {
    private int numColumns;
    private AttrType[] type;
    private Heapfile[] heapfiles;
    private String name;
    public Columnarfile(String name, int numColumns, AttrType[] type ) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = numColumns;
        this.type = type;
        this.name = name;
        this.heapfiles = new Heapfile[numColumns];

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
            this.type=new AttrType[numColumns];
            for(int i = 0; i<numColumns;i++){
                int attrType = Convert.getIntValue(4 + i * 4, data);
                this.type[i] = new AttrType(attrType);
            }
        }

        sc.closescan();
        this.heapfiles = new Heapfile[numColumns];
        for(int i = 0; i<numColumns;i++){
            this.heapfiles[i] = new Heapfile(this.name+"." + i);
        }


    }


    private void createHeaderFile() throws IOException, HFDiskMgrException, HFException, HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile hdrf = new Heapfile(this.name+".hdr");
        byte[] metaByte = new byte[4+4*numColumns];
        Convert.setIntValue(numColumns,0,metaByte);
        for(int i = 0; i < numColumns; i++){
            Convert.setIntValue(type[i].attrType,4+i*4,metaByte);
        }

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



}
