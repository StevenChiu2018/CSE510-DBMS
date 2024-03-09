package columnar;
import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
import btree.*;
import bitmap.*;
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

    boolean createBTreeIndex(int column){
        //if it doesn’t exist, create a BTree index for the given column

        // //how can i check if btree file exist or not(get_file_entry() is a private func)

        //DeleteFashion.NAIVE_DELETE = 0;

        int keyType = type[column - 1].attrType;
        int keySize = getKeySize(column);
        BTreeFile file=new BTreeFile(getBtreeFileName(column),keyType,keySize,DeleteFashion.NAIVE_DELETE); 
        
        Scan columnScan = openColumnScan(column);
        RID rid = new RID();
        Tuple tuple;
        while (true) {
            tuple = columnScan.getNext(rid);
            if (tuple == null) {
                break;
            }
            KeyClass key = ; //keytype is different based on tuple
            file.insert(key,tuple);
        }
        columnScan.closescan();
        file.close();
        return true;
        
    }
    boolean createBitMapIndex(int columnNo, valueClass value){
        // if it doesn’t exist, create a bitmap index for the given column
        //and value

        //how can i check if bitmap file exist or not(get_file_entry() is a private func)

        BitMapFile file = new BitMapFile(getBitMapFileName(columnNo,value),this,columnNo,value); 
        Tuple tuple;
        TID tid = new TID();
        TupleScan scan = new openTupleScan(tid); //how to get tid
        int position = 0;
        while (true) {
            tuple = columnScan.get_next();
            if (tuple == null) {
                break;
            }
            file.insert(position,1); //? what is the position // <value,TID> ->position?
        }
        scan.closetuplescan();
        file.close();

        return true;

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
        Scan scan = new Scan(this.deletedTupleList);
        RID rid = new RID();
        Tuple tuple;
        ArrayList<Integer> tidDeleted = new ArrayList<Integer>();
        while (true) {
            tuple = scan.getNext(rid);
            if (tuple == null) {
                break;
            }
            //add tid into tidarraylsit
            tidArrayList.add(tid);
        }
        for(TID tid : tidArrayList){
            for(int j=0;j<this.numColumns;j++){
              columnFiles[j].deleteRecord(tid.recordIDs[j]);
            }
        }
        //
        //also delete for index file
        //
        Scan scan = new Scan(this.deletedTupleList);
        RID rid = new RID();
        Tuple tuple;
        while (true) {
            tuple = scan.getNext(rid);
            if (tuple == null) {
                break;
            }
            deletedTupleList.deleteRecord(rid);
        }

        scan.closescan();
        return true;
    }

    private String getBitMapFileName(int columnNo, ValueClass value) {
        return "BM_" + value.toString() + "_" + this.name + "." + columnNo;
    }

    private String getBtreeFileName(int columnNo) {
        return "BT_" + this.name + "." + columnNo;
    }
    
    int getKeySize(int column) {
        int strPtr = 0;
        for (int i = 0; i < column - 1; i++) {
          if (type[i].attrType == AttrType.attrString) {
            strPtr++;
          }
        }
    
        AttrType attrType = type[column - 1];
        int keySize = 0;
    
        switch (attrType.attrType) {
          case AttrType.attrInteger:
            keySize = 4;
            break;
          case AttrType.attrReal:
            keySize = 4;
            break;
          case AttrType.attrString:
            keySize = strSizes[strPtr];
        }
    
        return keySize;
    }

}