package columnar;
import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
import btree.*;
class Columnarfile {
    private static int numColumns;
    private AttrType[] type;
    private Heapfile[] heapfiles;
    private String name;
    public Columnarfile(String name, int numColumns, AttrType[] type ) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = numColumns;
        this.type = type;
        this.name = name;
        this.heapfiles = new Heapfile[numColumns];
        this.deleteheapfiles = new Heapfile[numColumns];
        this.deletedTupleList = new Heapfile[numColumns];

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
    boolean updateTuple(TID tid, Tuple newtuple){
    //Updates the specified record in the columnar file.
        
    }
    boolean updateColumnofTuple(TID tid, Tuple newtuple, int column){
    //Updates the specified column of the specified record in the
    //columnar file
    }


    boolean createBTreeIndex(int column){
        //if it doesn’t exist, create a BTree index for the given column
        BTreeFile file=new BTreeFile(Integer.toString(column)); 
        file.traceFilename("TRACE");
        
        scan = this.Scan(column);
        KeyClass key;
        RID rid=new RID();
        Tuple temp;
        Tuple t = new Tuple();
        try {
            temp = scan.getNext(rid);
            } 
        catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
        }
        while (temp != null) {
            t.tupleCopy(temp);
    
            try {
                key = t.getStrFld(2); //not sure
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
    
            try {
                btf.insert(new StringKey(key), rid);
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
    
            try {
                temp = scan.getNext(rid);
            } 
            catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
        }
    
        // close the file scan
        scan.closescan();
        
    }
    boolean createBitMapIndex(int columnNo, valueClass value){
        // if it doesn’t exist, create a bitmap index for the given column
        //and value

        Bit file=new BTreeFile(Integer.toString(column)); 
        file.traceFilename("TRACE");
        
        scan = this.Scan(column);
        KeyClass key;
        RID rid=new RID();
        Tuple temp;
        Tuple t = new Tuple();
        try {
            temp = scan.getNext(rid);
            } 
        catch (Exception e) {
            status = FAIL;
            e.printStackTrace();
        }
        while (temp != null) {
            t.tupleCopy(temp);
    
            try {
                key = t.getStrFld(2); //not sure
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
    
            try {
                btf.insert(new StringKey(key), rid);
            } catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
    
            try {
                temp = scan.getNext(rid);
            } 
            catch (Exception e) {
                status = FAIL;
                e.printStackTrace();
            }
        }
    
        // close the file scan
        scan.closescan();

    }
    boolean markTupleDeleted(TID tid){
        //add the tuple to a heapfile tracking the deleted tuples from
        //the columnar file
        //Q : do I need to delete the tuple in this function?

        byte[] deleteTuple = new byte[];
        tid.recordIDs.writeToByteArray(deleteTuple,0);
        //if(!heapFileColumns[i].deleteRecord(tid.recordIDs[i]))
		//	return false;
        deletedTupleList.insertRecord(deleteTuple);

        return true;
    }
    
    boolean purgeAllDeletedTuples(){
        //merge all deleted tuples from the file as well as all from all
        //index files.

    }
    

}