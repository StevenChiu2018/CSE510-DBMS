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
    private Heapfile[] deletedTupleList;
    private BTreeFile[] bTreeFiles;
    private BitMapFile[] bitmapFiles;
    private String name;
    public Columnarfile(String name, int numColumns, AttrType[] type ) throws IOException, HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = numColumns;
        this.type = type;
        this.name = name;
        this.heapfiles = new Heapfile[numColumns];
        this.deletedTupleList = new Heapfile[numColumns];
        this.bTreeFiles = new BTreeFile[numColumns];
        this.bitmapFiles = new BitMapFile[numColumns];

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
            KeyClass key = KeyGetValue.getKeyClass(tuple.getTupleByteArray(),keyType,keySize); //keytype is different based on tuple
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
        String bmf = getBitMapFileName(value);
        if(bmf.get_file_entry()){ //not exist
            return true;
        }
        BitMapFile file = new BitMapFile(bmf,this,columnNo,value);
        return true;

    }
    boolean markTupleDeleted(TID tid){
        //add the tuple to a heapfile tracking the deleted tuples from
        //the columnar file

        byte[] deleteTuple = new byte[];
        tid.writeToByteArray(deleteTuple,0);
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
            //?不太確定存進去的是tuple是否就會每次scan都返回一樣的tuple
            TID tid = new TID();
            tid.getFromByteArray(tuple.getTupleByteArray());
            tidArrayList.add(tid);
        }
        for(TID tid : tidArrayList){
            for(int j = 0; j < this.numColumns; j++){
                for(int k = 0;k< tid.numRIDs;k++){
                    columnFiles[j].deleteRecord(tid.recordIDs[k]);
                    if(bTreeFiles[j]!=null){ //file exist
                        int keyType = type[j - 1].attrType;
                        Tuple tupleB = getColumn(i).getRecord(position);
                        int keySize = getKeySize(j);
                        KeyClass key = KeyGetValue.getKeyClass(tupleB.getTupleByteArray(),keyType,keySize); 
                        bTreeFiles[j].Delete(key,tid.recordIDs[j]);
                        bTreeFiles[j].close();
                    }
                    String bmfs = getBitMapFileName(columnFiles[k].getRecord(tid.recordIDs[j]));
                    if(bmfs.get_file_entry()){ //not exist
                        break;
                    }
                    BitMapFile bmf = BitMapFile(bmfs);
                    bmf.Delete(tid.position);
                    bmf.close();
                }
            }
            for(int k = 0;k< tid.numRIDs;k++)
                TidFile.deleteRecord(tid.recordIDs[k]);
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