package columnar;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.SecureDirectoryStream;
import java.util.ArrayList;
import java.util.Arrays;

import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;

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
        this.columns = new Heapfile[numColumns];
        this.tupleLength = 0;
        this.tidHeap = new Heapfile(name + "-TIDs");

        createHeaderFile();
        createColumnInfoHeapfile();

        for (int i = 0; i < numColumns; i++) {
            this.tupleLength += columnsInfo[i].sizeInBytes;
            columns[i] = new Heapfile(columnsInfo[i].fileName);
        }
    }

    public Columnarfile(String name) throws HFDiskMgrException, HFException, HFBufMgrException,
            InvalidTupleSizeException, IOException {
        loadHeaderFile(name);
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

}
