package columnar;

import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
import btree.*;
import bitmap.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

class Columnarfile {
    public int numColumns;
    public String name;
    public int tupleLength;
    public int tidPositionCount = 0;

    private BTreeFile[] bTreeFiles;

    public ColumnInfo[] columnsInfo;
    public Heapfile[] columns;
    public Heapfile tidHeap;



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

    //add to columnar constructor
    Short[] strSizes = new short[2];
    strSizes[0] = 50;
    strSizes[1] = 50;
    public short[] getStrSizes(){
        return strSizes;
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
    
        Heapfile headerfile = new Heapfile(this.name);
        Scan sc = headerfile.openScan();
        RID rid = new RID();
        Tuple tuple;
        byte[] result;

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        this.name = Convert.getStrValue(0, result, 100);

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        String tidFileName = Convert.getStrValue(0, result, 100);
        this.tidHeap = new Heapfile(tidFileName);

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        String columnInfoName = Convert.getStrValue(0, result, 100);

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        this.tidPositionCount = Convert.getIntValue(0, result);

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        this.numColumns = Convert.getIntValue(0, result);

        tuple = sc.getNext(rid);
        result = tuple.getTupleByteArray();
        this.tupleLength = Convert.getIntValue(0, result);
    
        loadColumnInfo(columnInfoName);
    }
    private void loadColumnInfo(String columnInfoName) throws InvalidTupleSizeException, IOException, HFDiskMgrException, HFException, HFBufMgrException {
        Heapfile headerfile = new Heapfile(this.name);
        Scan sc = headerfile.openScan();
        RID rid = new RID();
        Tuple tuple;
        byte[] result;
        this.columnsInfo = new ColumnInfo[numColumns];
        for(int i=0;(tuple = sc.getNext(rid)) != null;i++){
            this.columnsInfo[i] = new ColumnInfo(tuple.getTupleByteArray());
        }
    }

    public Columnarfile(String name) throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException, IOException {
        loadHeaderFile();
    }


    public void createHeaderFile() throws IOException, HFDiskMgrException, HFException, HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile headerFile = new Heapfile(this.name);

        byte[] nameByte = new byte[100];
        Convert.setStrValue(name, 0, nameByte);
        headerFile.insertRecord(nameByte);

        byte[] tidFileNameByte = new byte[100];
        Convert.setStrValue(name + "-TIDs", 0, tidFileNameByte);
        headerFile.insertRecord(tidFileNameByte);

        byte[] columnInfoNameByte = new byte[100];
        Convert.setStrValue(name+"-column-info", 0, columnInfoNameByte);
        headerFile.insertRecord(columnInfoNameByte);

        byte[] tidPositionCountByte = new byte[4];
        Convert.setIntValue(tidPositionCount, 0, nameByte);
        headerFile.insertRecord(tidPositionCountByte);

        byte[] numberColumnByte = new byte[4];
        Convert.setIntValue(numColumns, 0, numberColumnByte);
        headerFile.insertRecord(numberColumnByte);

        byte[] tupleLengthByte = new byte[4];
        Convert.setIntValue(tupleLength, 0, tupleLengthByte );
        headerFile.insertRecord(tupleLengthByte );
    }
    // serialize col Info into byte array and store rec in heapfile
    public void createColumnInfoHeapfile() throws HFDiskMgrException, HFException, HFBufMgrException, IOException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile columnInfoHeap = new Heapfile(this.name + "-column-info");
        for(ColumnInfo col : columnsInfo){
            byte[] buffer = new byte[col.calculateSpace()];
            col.writeToByteArray(buffer,0);
            columnInfoHeap.insertRecord(buffer);
        }
    }

    public void deleteColumnarFile() throws HFDiskMgrException, InvalidSlotNumberException, InvalidTupleSizeException, HFBufMgrException, FileAlreadyDeletedException, IOException, FileEntryNotFoundException, InvalidPageNumberException, FileIOException, DiskMgrException, HFException {
        //delete all columns
        for(int i = 0; i<numColumns;i++){
            this.columns[i].deleteFile();
        }
        String columnInfoFileName = this.name + "-column-info";
        new Heapfile(columnInfoFileName).deleteFile();

        if(tidHeap!=null){
            tidHeap.deleteFile();
        }

        String header_name = this.name+".hdr";
        SystemDefs.JavabaseDB.delete_file_entry(header_name);

    }
    public TID insertTuple(byte[] tuplePtr) throws HFDiskMgrException, HFException, HFBufMgrException, IOException, SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        TID tid = new TID(numColumns, this.tidPositionCount);
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

        byte[] tidRawData= new byte[8+(8*numColumns)];
        tid.writeToByteArray(tidRawData, 0);
        tidHeap.insertRecord(tidRawData);
        tidPositionCount++;
        //tid.setPosition(tidPositionCount);
        return tid;
    }



    
    boolean createBTreeIndex(int column) {
        // if it doesn’t exist, create a BTree index for the given column

        // //how can i check if btree file exist or not(get_file_entry() is a private func)

        // DeleteFashion.NAIVE_DELETE = 0;

        int keyType = columnsInfo[column - 1].type.attrType;
        int keySize = getKeySize(column);
        BTreeFile file = new BTreeFile(getBtreeFileName(column), keyType, keySize,
                DeleteFashion.NAIVE_DELETE);
        System.out.println("keytype: " + keyType);
        System.out.println("keysize: " + keySize);

        Scan columnScan = openColumnScan(column);
        RID rid = new RID();
        Tuple tuple;
        while (true) {
            tuple = columnScan.getNext(rid);
            if (tuple == null) {
                break;
            }
            KeyClass key = KeyGetValue.getKeyClass(tuple.getTupleByteArray(), columnsInfo[column - 1].type, keySize); 
            file.insert(key, rid);
        }
        columnScan.closescan();
        file.close();
        return true;

    }

    boolean createBitMapIndex(int columnNo, ValueClass value) {
        // if it doesn’t exist, create a bitmap index for the given column
        // and value

        // how can i check if bitmap file exist or not(get_file_entry() is a private func)
        String bmf = getBitMapFileName_value(columnNo, value);
        if (bmf.get_file_entry()) { // not exist
            return true;
        }
        BitMapFile file = new BitMapFile(bmf, this, columnNo, value);
        return true;

    }

    boolean markTupleDeleted(TID tid){
        //add the tuple to a heapfile tracking the deleted tuples from
        //the columnar file

        // byte[] deleteTuple = new byte[];
        // tid.writeToByteArray(deleteTuple,0);
        // deletedTupleList.insertRecord(deleteTuple);

        for(int j = 0; j < this.numColumns; j++){
            //Btree delete
            if(bTreeFiles[j]!=null){
                Tuple tupleB = columns[j].getRecord(tid.recordIDs[j]); //?
                AttrType keyType = columnsInfo[j - 1].type;
                int keySize = getKeySize(j);
                KeyClass key = KeyGetValue.getKeyClass(tupleB.getTupleByteArray(),keyType,keySize); 
                bTreeFiles[j].Delete(key,tid.recordIDs[j]);
                bTreeFiles[j].close();
            }

            //BitMap delete 
            String bmfs = getBitMapFileName_tuple(j,columns[j].getRecord(tid.recordIDs[j])); //不太確定value轉string會部會跟tuple轉string一樣
            if(bmfs.get_file_entry()){ //not exist
                break;
            }
            BitMapFile bmf = BitMapFile(bmfs);
            bmf.Delete(tid.position);
            bmf.close();

            //columnarfile delte
            columns[j].deleteRecord(tid.recordIDs[j]);
        }
        return true;
    }

    boolean purgeAllDeletedTuples() {
        // merge all deleted tuples from the file as well as all from all
        // index files.
        return true;

    }

    private String getBitMapFileName_value(int columnNo, ValueClass value) {
        return "BM_" + value.toString() + "_" + this.name + "." + columnNo;
    }

    private String getBitMapFileName_tuple(int columnNo, Tuple tuple) {
        return "BM_" + tuple.toString() + "_" + this.name + "." + columnNo;
    }

    private String getBtreeFileName(int columnNo) {
        return "BT_" + this.name + "." + columnNo;
    }


}
