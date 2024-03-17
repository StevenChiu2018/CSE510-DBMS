package columnar;

import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
import btree.*;
import bitmap.*;
import bitmap.AddFileEntryException;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bitmap.UnpinPageException;
import java.nio.file.SecureDirectoryStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Columnarfile {
    public int numColumns;
    public String name;
    public int tupleLength;
    public int tidPositionCount = 0;

    public ColumnInfo[] columnsInfo;
    public Heapfile[] columns;
    public Heapfile tidHeap;

    public Columnarfile(String name, ColumnInfo[] columnsInfo)
            throws IOException, HFException, HFBufMgrException, HFDiskMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        this.numColumns = columnsInfo.length;
        this.columnsInfo = columnsInfo;
        this.name = name;
        this.tupleLength = 0;
        this.tidHeap = new Heapfile(name + "-TIDs");
        createHeaderFile();
        createColumnInfoHeapfile();
        constructColumns();
    }

    public Columnarfile(String name)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            IOException, SpaceNotAvailableException, InvalidSlotNumberException {
        loadHeaderFile(name);
    }

    private void constructColumns()
            throws HFException, HFBufMgrException, HFDiskMgrException, IOException {
        this.columns = new Heapfile[numColumns];
        for (int i = 0; i < numColumns; i++) {
            this.tupleLength += columnsInfo[i].sizeInBytes;
            columns[i] = new Heapfile(columnsInfo[i].fileName);
        }
    }

    public boolean isFileExist(String name) {
        try {
            PageId pageid = get_file_entry(name);
            return pageid != null;
        } catch (Exception e) {
            return false;
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

    public void loadHeaderFile(String fileName) throws HFDiskMgrException, HFException,
            HFBufMgrException, IOException, InvalidTupleSizeException {
        Heapfile headerfile = new Heapfile(fileName);
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
        constructColumns();
    }

    private void loadColumnInfo(String columnInfoName) throws InvalidTupleSizeException,
            IOException, HFDiskMgrException, HFException, HFBufMgrException {
        Heapfile headerfile = new Heapfile(columnInfoName);
        Scan sc = headerfile.openScan();
        RID rid = new RID();
        Tuple tuple;
        this.columnsInfo = new ColumnInfo[this.numColumns];
        for (int i = 0; (tuple = sc.getNext(rid)) != null; i++) {
            this.columnsInfo[i] = new ColumnInfo(tuple.getTupleByteArray());
        }
    }

    public void createHeaderFile()
            throws IOException, HFDiskMgrException, HFException, HFBufMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile headerFile = new Heapfile(this.name);

        byte[] nameByte = new byte[100];
        Convert.setStrValue(name, 0, nameByte);
        headerFile.insertRecord(nameByte);

        byte[] tidFileNameByte = new byte[100];
        Convert.setStrValue(name + "-TIDs", 0, tidFileNameByte);
        headerFile.insertRecord(tidFileNameByte);

        byte[] columnInfoNameByte = new byte[100];
        Convert.setStrValue(name + "-column-info", 0, columnInfoNameByte);
        headerFile.insertRecord(columnInfoNameByte);

        byte[] tidPositionCountByte = new byte[4];
        Convert.setIntValue(tidPositionCount, 0, nameByte);
        headerFile.insertRecord(tidPositionCountByte);

        byte[] numberColumnByte = new byte[4];
        Convert.setIntValue(numColumns, 0, numberColumnByte);
        headerFile.insertRecord(numberColumnByte);

        byte[] tupleLengthByte = new byte[4];
        Convert.setIntValue(tupleLength, 0, tupleLengthByte);
        headerFile.insertRecord(tupleLengthByte);
    }

    // serialize col Info into byte array and store rec in heapfile
    public void createColumnInfoHeapfile()
            throws HFDiskMgrException, HFException, HFBufMgrException, IOException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Heapfile columnInfoHeap = new Heapfile(this.name + "-column-info");
        for (ColumnInfo col : columnsInfo) {
            byte[] buffer = new byte[col.calculateSpace()];
            col.writeToByteArray(buffer, 0);
            columnInfoHeap.insertRecord(buffer);
        }
    }

    public void deleteColumnarFile()
            throws HFDiskMgrException, InvalidSlotNumberException, InvalidTupleSizeException,
            HFBufMgrException, FileAlreadyDeletedException, IOException, FileEntryNotFoundException,
            InvalidPageNumberException, FileIOException, DiskMgrException, HFException {
        // delete all columns
        for (int i = 0; i < numColumns; i++) {
            this.columns[i].deleteFile();
        }
        String columnInfoFileName = this.name + "-column-info";
        new Heapfile(columnInfoFileName).deleteFile();

        if (tidHeap != null) {
            tidHeap.deleteFile();
        }

        String header_name = this.name + ".hdr";
        SystemDefs.JavabaseDB.delete_file_entry(header_name);

    }

    public TID insertTuple(byte[] rawTuple)
            throws HFDiskMgrException, HFException, HFBufMgrException, IOException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        TID tid = new TID(numColumns);
        // offset for next block..
        int offset = 0;

        for (int i = 0; i < numColumns; i++) {
            byte[] colData;
            switch (columnsInfo[i].type.attrType) {
                case AttrType.attrInteger:
                    colData = Arrays.copyOfRange(rawTuple, offset, offset + 4);
                    break;
                case AttrType.attrString:
                    colData = Arrays.copyOfRange(rawTuple, offset,
                            offset + columnsInfo[i].sizeInBytes);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            offset += columnsInfo[i].sizeInBytes;

            RID newrid = this.columns[i].insertRecord(colData);
            tid.setRID(i, newrid);
        }

        byte[] tidRawData = new byte[8 + (8 * numColumns)];
        tid.position = this.tidPositionCount++;
        tid.writeToByteArray(tidRawData, 0);
        tidHeap.insertRecord(tidRawData);


        return tid;
    }

    // Read the tuple with the given tid from the columnar file
    public Tuple getTuple(TID tid) {
        byte[] tuple = new byte[tupleLength];
        int offset = 0;
        int length = 0;

        Tuple t = new Tuple();

        try {
            for (int i = 0; i < numColumns; i++) {

                t = columns[i].getRecord(tid.recordIDs[i]);

                if (columnsInfo[i].type.attrType == AttrType.attrInteger) {
                    int value = Convert.getIntValue(offset, t.returnTupleByteArray());
                    Convert.setIntValue(value, offset, tuple);
                    offset = offset + 4;
                    length += 4;
                }

                if (columnsInfo[i].type.attrType == AttrType.attrString) {
                    String value = Convert.getStrValue(offset, t.returnTupleByteArray(),
                            columnsInfo[i].sizeInBytes);
                    Convert.setStrValue(value, offset, tuple);
                    offset = offset + columnsInfo[i].sizeInBytes;
                    length += columnsInfo[i].sizeInBytes;
                }
            }
            t.tupleSet(tuple, 0, length);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    // Read the value with the given column and tid from the columnar file
    public ValueClass getValue(TID tid, int column) {

        ValueClass value = null;
        IntegerValue integer = new IntegerValue();
        StringValue str = new StringValue();

        try {
            byte[] colValue =
                    columns[column].getRecord(tid.recordIDs[column]).returnTupleByteArray();

            if (columnsInfo[column].type.attrType == AttrType.attrInteger) {

                integer.setValue(Convert.getIntValue(0, colValue));
                value = integer;
            } else if (columnsInfo[column].type.attrType == AttrType.attrString) {

                str.setValue(Convert.getStrValue(0, colValue, columnsInfo[column].sizeInBytes));
                value = str;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    // Return the number of tuples in the columnar file.
    public int getTupleCnt()
            throws InvalidSlotNumberException, InvalidTupleSizeException, IOException {

        int count = 0;
        try {
            count = columns[0].getRecCnt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    // Initiate a sequential scan of tuples.
    public TupleScan openTupleScan() throws InvalidTupleSizeException, IOException {
        TupleScan scan = new TupleScan(this);
        return scan;
    }

    // Initiate a sequential scan along a given column.
    public Scan openColumnScan(int columnNo) {
        Scan scan = null;
        try {
            scan = new Scan(columns[columnNo]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scan;
    }

    boolean createBTreeIndex(int column) {
        // if it doesn’t exist, create a BTree index for the given column

        // DeleteFashion.NAIVE_DELETE = 0;

        int keyType = columnsInfo[column - 1].type.attrType;
        int keySize = this.columnsInfo[column].sizeInBytes;
        BTreeFile file = null;
        try {
            file = new BTreeFile(getBtreeFileName(column), keyType, keySize,
                    DeleteFashion.NAIVE_DELETE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("keytype: " + keyType);
        System.out.println("keysize: " + keySize);

        Scan columnScan = openColumnScan(column);
        RID rid = new RID();
        Tuple tuple = null;
        while (true) {
            try {
                tuple = columnScan.getNext(rid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (tuple == null) {
                break;
            }
            try {
                KeyClass key = KeyGetValue.getKeyClass(tuple.getTupleByteArray(),
                        columnsInfo[column - 1].type, keySize);

                try {
                    file.insert(key, rid);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        columnScan.closescan();
        try {
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    boolean createBitMapIndex(int columnNo, ByteValue value) throws HFDiskMgrException,
            UnpinPageException, PinPageException, InvalidTupleSizeException {
        // if it doesn’t exist, create a bitmap index for the given column
        // and value

        String bmf = getBitMapFileName(columnNo, value.value);
        try {
            new BitMapFile(bmf, this, columnNo, value);
        } catch (GetFileEntryException | ConstructPageException | IOException
                | AddFileEntryException e) {
            e.printStackTrace();
        }
        return true;

    }

    boolean markTupleDeleted(TID tid) throws InvalidSlotNumberException, InvalidTupleSizeException,
            HFException, HFDiskMgrException, HFBufMgrException, Exception {
        // add the tuple to a heapfile tracking the deleted tuples from
        // the columnar file

        // byte[] deleteTuple = new byte[];
        // tid.writeToByteArray(deleteTuple,0);
        // deletedTupleList.insertRecord(deleteTuple);

        for (int j = 0; j < this.numColumns; j++) {
            // Btree delete
            int keyType = columnsInfo[j - 1].type.attrType;
            int keySize = this.columnsInfo[j].sizeInBytes;
            BTreeFile file = null;
            Tuple tupleB = null;
            KeyClass key = null;
            try {
                file = new BTreeFile(getBtreeFileName(j), keyType, keySize,
                        DeleteFashion.NAIVE_DELETE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                tupleB = columns[j].getRecord(tid.recordIDs[j]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                key = KeyGetValue.getKeyClass(tupleB.getTupleByteArray(), columnsInfo[j - 1].type,
                        keySize);

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                file.Delete(key, tid.recordIDs[j]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                file.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // //BitMap delete
            byte[] barray = columns[j].getRecord(tid.recordIDs[j]).getTupleByteArray();
            byte[] Barray = new byte[barray.length];
            for (int i = 0; i < barray.length; i++) {
                Barray[i] = barray[i];
            }
            String bmfs = getBitMapFileName(j, Barray); // 都轉成byte array
            BitMapFile file2 = null;
            try {
                file2 = new BitMapFile(bmfs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            file2.delete(tid.position);
            file2.close();

            // columnarfile delte
            try {
                columns[j].deleteRecord(tid.recordIDs[j]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // columns[j].tidHeap.deleteRecord(tid.recordIDs[j]);
        }
        return true;
    }

    boolean purgeAllDeletedTuples() {
        // merge all deleted tuples from the file as well as all from all
        // index files.
        return true;

    }

    private String getBitMapFileName(int columnNo, byte[] value) {
        return "BM_" + value.toString() + "_" + this.name + "." + columnNo;
    }

    private String getBtreeFileName(int columnNo) {
        return "BT_" + this.name + "." + columnNo;
    }

    // Update the specified record in the columnar file.
    public boolean updateTuple(TID tid, Tuple newRowTuple) {
        int offset = 0;
        byte[] columnByteArray;
        byte[] byteArray = newRowTuple.getTupleByteArray();

        for (int i = 0; i < numColumns; i++) {
            columnByteArray = new byte[columnsInfo[i].sizeInBytes];
            System.arraycopy(byteArray, offset, columnByteArray, 0, columnsInfo[i].sizeInBytes);
            offset += columnsInfo[i].sizeInBytes;

            Tuple newTuple = new Tuple(columnByteArray, 0, columnsInfo[i].sizeInBytes);
            if (updateColumnofTuple(tid, newTuple, i) == false) {
                return false;
            }
        }

        return true;
    }

    // Update the specified column of the specified record in the columnar file.
    public boolean updateColumnofTuple(TID tid, Tuple newtuple, int column) {
        int intValue;
        String strValue;
        Tuple tuple = null;
        try {
            if (columnsInfo[column].type.attrType == AttrType.attrInteger) {
                intValue = newtuple.getIntFld(column);
                tuple = new Tuple(4);
                tuple.setIntFld(1, intValue);
            } else if (columnsInfo[column].type.attrType == AttrType.attrString) {
                strValue = newtuple.getStrFld(column);
                tuple = new Tuple(columnsInfo[column].sizeInBytes);
                tuple.setStrFld(1, strValue);
            }

            return columns[column].updateRecord(tid.recordIDs[column], tuple);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
