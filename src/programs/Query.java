package programs;

import java.io.IOException;
import java.util.ArrayList;
import bitmap.BitMapFile;
import bitmap.BitMapHeaderPage;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bufmgr.PageNotReadException;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import columnar.TupleScan;
import global.AttrOperator;
import global.AttrType;
import global.Convert;
import global.IndexType;
import global.RID;
import global.SystemDefs;
import global.TID;
import heap.FieldNumberOutOfBoundException;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.Heapfile;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.InvalidTypeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import index.ColumnarIndexScan;
import index.IndexException;
import index.IndexScan;
import index.UnknownIndexTypeException;
import iterator.ColumnarFileScan;
import iterator.CondExpr;
import iterator.FileScanException;
import iterator.FldSpec;
import iterator.InvalidRelation;
import iterator.JoinsException;
import iterator.PredEvalException;
import iterator.RelSpec;
import iterator.TupleUtilsException;
import iterator.UnknowAttrType;
import iterator.UnknownKeyTypeException;
import iterator.WrongPermat;

public class Query {
    public static void main(String[] args)
            throws HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException,
            InvalidSlotNumberException, InvalidTupleSizeException, IOException, FileScanException,
            TupleUtilsException, InvalidRelation, JoinsException, InvalidTypeException,
            PageNotReadException, PredEvalException, UnknowAttrType, FieldNumberOutOfBoundException,
            WrongPermat, IndexException, UnknownIndexTypeException, UnknownKeyTypeException,
            GetFileEntryException, PinPageException, ConstructPageException {
        if (!isValidInput(args)) {
            System.out.println(
                    "query [:COLUMNDBNAME] [:COLUMNARFILENAME] [:TARGETCOLUMNNAMES] [:VALUECONSTRAINT] [:NUMBUF] [:ACCESSTYPE]");
            return;
        }

        String[] targetColumnNames = new String[0];
        if (args.length > 6) {
            targetColumnNames = args[3].split(",");
        }

        ValueConstraint valueConstraint = new ValueConstraint(args[args.length - 3]);
        int numBuf = Integer.parseInt(args[args.length - 2]);

        execute(args[1], args[2], targetColumnNames, valueConstraint, numBuf,
                args[args.length - 1]);

        return;
    }

    private static boolean isValidInput(String[] args) {
        return args.length >= 6 && args[0].equals("query");
    }

    public static boolean execute(String columnDBName, String columnarFileName,
            String[] targetColumnNames, ValueConstraint valueConstraint, int numBuf,
            String accessType) throws IOException, HFException, HFBufMgrException,
            HFDiskMgrException, SpaceNotAvailableException, InvalidSlotNumberException,
            InvalidTupleSizeException, FileScanException, TupleUtilsException, InvalidRelation,
            JoinsException, InvalidTypeException, PageNotReadException, PredEvalException,
            UnknowAttrType, FieldNumberOutOfBoundException, WrongPermat, IndexException,
            UnknownIndexTypeException, UnknownKeyTypeException, GetFileEntryException,
            PinPageException, ConstructPageException {
        new SystemDefs(columnDBName, 0, numBuf, null);
        Tuple[] scanResult = new Tuple[0];
        boolean needSelect = true;

        switch (accessType) {
            case "FILESCAN":
                scanResult = doFileScan(columnarFileName, valueConstraint);
                break;

            case "COLUMNSCAN":
                scanResult = doColumnScan(columnarFileName, valueConstraint);
                break;

            // case "BTREE":
            // scanResult = doBtreeScan(columnarFileName, valueConstraint);
            // targetColumnNames = new String[] {valueConstraint.columnName};
            // needSelect = false;
            // break;

            case "BITMAP":
                scanResult = doBitMapScan(columnarFileName, valueConstraint);
                break;

            default:
                break;
        }

        printResult(scanResult, columnarFileName, targetColumnNames, needSelect);

        return true;
    }

    // private static void forTest(String columnarFileName)
    // throws HFException, HFBufMgrException, HFDiskMgrException, IOException,
    // InvalidTupleSizeException, SpaceNotAvailableException, InvalidSlotNumberException {
    // Columnarfile columnarFile = new Columnarfile(columnarFileName);
    // // Heapfile tidHeap = new Heapfile(columnarFileName + "-TIDs");
    // Scan scanner = columnarFile.tidHeap.openScan();
    // RID rid = new RID();
    // Tuple result;

    // while ((result = scanner.getNext(rid)) != null) {
    // System.out.println(result);
    // }
    // }

    private static Tuple[] doFileScan(String columnarFileName, ValueConstraint valueConstraint)
            throws FileScanException, TupleUtilsException, InvalidRelation, IOException,
            HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, JoinsException,
            InvalidTypeException, PageNotReadException, PredEvalException, UnknowAttrType,
            FieldNumberOutOfBoundException, WrongPermat {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        Scan scanner = columnarFile.tidHeap.openScan();
        ArrayList<Tuple> result = new ArrayList<Tuple>();
        RID rid = new RID();
        Tuple curResult;
        while ((curResult = scanner.getNext(rid)) != null) {
            TID rowTID = new TID(0, curResult.getTupleByteArray());
            Tuple rowTuple = columnarFile.getTuple(rowTID);

            if (comparedResult(columnarFile, valueConstraint, rowTuple)) {
                result.add(rowTuple);
            }
        }

        return result.toArray(new Tuple[0]);
    }

    private static Tuple[] doColumnScan(String columnarFileName, ValueConstraint valueConstraint)
            throws IOException, InvalidTupleSizeException, HFDiskMgrException, HFException,
            HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException {
        ArrayList<Tuple> scanResult = new ArrayList<Tuple>();
        Tuple compared;
        Tuple tidTuple;
        RID redundentRID = new RID();
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int constraintColumnNo =
                getColumnsNo(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo constraintColumnInfo = columnarFile.columnsInfo[constraintColumnNo];

        Scan columnScanner = columnarFile.columns[constraintColumnNo].openScan();
        Scan tidScanner = columnarFile.tidHeap.openScan();
        while ((compared = columnScanner.getNext(redundentRID)) != null) {
            tidTuple = tidScanner.getNext(redundentRID);

            boolean compareResult = false;
            if (constraintColumnInfo.type.attrType == 1) {
                int value = Convert.getIntValue(0, compared.getTupleByteArray());
                compareResult =
                        compareInt(value, valueConstraint.operator, valueConstraint.intValue);
            } else {
                String value = Convert.getStrValue(0, compared.getTupleByteArray(),
                        constraintColumnInfo.sizeInBytes);
                compareResult =
                        compareString(value, valueConstraint.operator, valueConstraint.stringValue);
            }

            if (compareResult) {
                TID tid = new TID(0, tidTuple.getTupleByteArray());
                Tuple rowTuple = columnarFile.getTuple(tid);
                scanResult.add(rowTuple);
            }
        }

        return scanResult.toArray(new Tuple[0]);
    }

    private static int[] getColumnsNo(Columnarfile columnarFile, String[] columnNames) {
        int[] columnNos = new int[columnNames.length];

        for (int i = 0; i < columnNames.length; i++) {
            columnNos[i] = columnarFile.getColumnInfoByColumnName(columnNames[i]).columnNo;
        }

        return columnNos;
    }

    private static boolean comparedResult(Columnarfile columnarFile,
            ValueConstraint valueConstraint, Tuple rowTuple) throws IOException {
        ColumnInfo constraintColumnInfo = new ColumnInfo();
        int columnOffset = 0;


        for (int i = 0; i < columnarFile.columnsInfo.length; i++) {
            if (columnarFile.columnsInfo[i].columnName.equals(valueConstraint.columnName)) {
                constraintColumnInfo = columnarFile.columnsInfo[i];
                break;
            }

            columnOffset += columnarFile.columnsInfo[i].sizeInBytes;
        }

        if (constraintColumnInfo.type.attrType == AttrType.attrInteger) {
            int value = Convert.getIntValue(columnOffset, rowTuple.getTupleByteArray());

            return compareInt(value, valueConstraint.operator, valueConstraint.intValue);
        } else {
            String value = Convert.getStrValue(columnOffset, rowTuple.getTupleByteArray(),
                    constraintColumnInfo.sizeInBytes);

            return compareString(value, valueConstraint.operator, valueConstraint.stringValue);
        }
    }

    private static boolean compareInt(int val1, String operator, int val2) {
        switch (operator) {
            case ">":
                return val1 > val2;

            case "<":
                return val1 < val2;

            case "=":
                return val1 == val2;

            case ">=":
                return val1 >= val2;

            case "<=":
                return val1 <= val2;

            default:
                return false;
        }
    }

    private static boolean compareString(String val1, String operator, String val2) {
        switch (operator) {
            case "=":
                return val1.equals(val2);

            default:
                return false;
        }
    }

    // private static Tuple[] doBtreeScan(String columnarFileName, ValueConstraint valueConstraint)
    // {
    // Columnarfile columnarFile = new Columnarfile(columnarFileName);
    // int constraintColumnNo =
    // getColumnsNo(columnarFile, new String[] {valueConstraint.columnName})[0];
    // ColumnInfo columnInfo = columnarFile.columnsInfo[constraintColumnNo];
    // IndexType indexType = new IndexType(1);
    // String indexName = columnarFile.getBtreeFileName(constraintColumnNo);
    // AttrType[] types = new AttrType[] {columnInfo.type};
    // short[] stringSizes = new short[1];
    // if (columnInfo.type.attrType == AttrType.attrString) {
    // stringSizes[0] = (short) columnInfo.sizeInBytes;
    // } else {
    // stringSizes = new short[0];
    // }
    // RelSpec relSpec = new RelSpec(0);
    // FldSpec[] outFlds = new FldSpec[] {new FldSpec(relSpec, 0)};
    // CondExpr[] selects = getOutFilter(columnarFile, valueConstraint);

    // IndexScan scanner = new IndexScan(indexType, indexName + "-Btree-scanner", indexName, types,
    // stringSizes, 1, 1, outFlds, selects, 1, true);

    // ArrayList<Tuple> result = new ArrayList<Tuple>();
    // Tuple curResult;
    // while ((curResult = scanner.get_next()) != null) {
    // result.add(curResult);
    // }

    // return result.toArray(new Tuple[0]);
    // }

    private static Tuple[] doBitMapScan(String columnarFileName, ValueConstraint valueConstraint)
            throws IndexException, InvalidTypeException, InvalidTupleSizeException,
            UnknownIndexTypeException, IOException, UnknownKeyTypeException, HFDiskMgrException,
            HFException, HFBufMgrException, SpaceNotAvailableException, InvalidSlotNumberException,
            GetFileEntryException, PinPageException, ConstructPageException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int constraintColumnNo =
                getColumnsNo(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo constraintColumnInfo = columnarFile.columnsInfo[constraintColumnNo];
        IndexType[] indexTypes = new IndexType[] {new IndexType(3)};
        String[] indexNames = getIndexName(columnarFile, constraintColumnInfo, valueConstraint);
        AttrType[] types = getIndexTypes(constraintColumnInfo, indexNames.length);
        short[] stringSizes = getIndexStringSizes(constraintColumnInfo, indexNames.length);

        ColumnarIndexScan scanner =
                new ColumnarIndexScan(columnarFileName, null, indexTypes, indexNames, types,
                        stringSizes, 1, columnarFile.columnsInfo.length, null, null, false);

        ArrayList<Tuple> result = new ArrayList<Tuple>();
        Tuple curResult;
        while ((curResult = scanner.get_next()) != null) {
            result.add(curResult);
        }

        return result.toArray(new Tuple[0]);
    }

    private static String[] getIndexName(Columnarfile columnarFile, ColumnInfo constraintColumnInfo,
            ValueConstraint valueConstraint) throws IOException, InvalidTupleSizeException,
            HFException, HFBufMgrException, HFDiskMgrException {
        ArrayList<String> indexNames = new ArrayList<String>();
        Scan scanner = constraintColumnInfo.bitmapFileName.openScan();
        RID rid = new RID();
        Tuple bitmapValueTuple;
        while ((bitmapValueTuple = scanner.getNext(rid)) != null) {
            if (constraintColumnInfo.type.attrType == AttrType.attrInteger) {
                byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                int value = Convert.getIntValue(0, bitmapValue);
                if (compareInt(value, valueConstraint.operator, valueConstraint.intValue)) {
                    indexNames.add(columnarFile.getBitMapFileName(constraintColumnInfo.columnNo,
                            Integer.toString(value)));
                }
            } else {
                byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                String value =
                        Convert.getStrValue(0, bitmapValue, constraintColumnInfo.sizeInBytes);
                if (compareString(value, valueConstraint.operator, valueConstraint.stringValue)) {
                    indexNames.add(
                            columnarFile.getBitMapFileName(constraintColumnInfo.columnNo, value));
                }
            }
        }

        return indexNames.toArray(new String[0]);
    }

    private static AttrType[] getIndexTypes(ColumnInfo constraintColumnInfo, int num) {
        AttrType[] types = new AttrType[num];
        for (int i = 0; i < num; i++) {
            types[i] = constraintColumnInfo.type;
        }

        return types;
    }

    private static short[] getIndexStringSizes(ColumnInfo constraintColumnInfo, int num) {
        short[] stringSizes = new short[num];
        for (int i = 0; i < num; i++) {
            if (constraintColumnInfo.type.attrType == AttrType.attrString) {
                stringSizes[i] = (short) constraintColumnInfo.sizeInBytes;
            } else {
                stringSizes[i] = 0;
            }
        }

        return stringSizes;
    }

    private static void printResult(Tuple[] sourceTuple, String columnarFileName,
            String[] targetColumnNames, boolean needSelect)
            throws IOException, HFException, HFBufMgrException, HFDiskMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        ColumnInfo[] columnsInfo = columnarFile.columnsInfo;
        int[] columnsNo = getTargetColumnNos(columnarFile, targetColumnNames);

        int[] columnsOffset = new int[columnsInfo.length];
        for (int i = 0, offset = 0; i < columnsOffset.length; i++) {
            columnsOffset[i] = offset;
            if (needSelect) {
                offset += columnsInfo[i].sizeInBytes;
            }
        }

        for (Tuple tuple : sourceTuple) {
            byte[] rawTuple = tuple.getTupleByteArray();
            for (int columnNo : columnsNo) {
                if (columnsInfo[columnNo].type.attrType == AttrType.attrInteger) {
                    int value = Convert.getIntValue(columnsOffset[columnNo], rawTuple);
                    System.out.print(value + ",");
                } else {
                    String value = Convert.getStrValue(columnsOffset[columnNo], rawTuple,
                            columnsInfo[columnNo].sizeInBytes);
                    System.out.print(value + ",");
                }
            }
            System.out.print("\n");
        }
    }

    private static int[] getTargetColumnNos(Columnarfile columnarfile, String[] targetColumnNames) {
        if (targetColumnNames.length == 0) {
            int[] result = new int[columnarfile.columnsInfo.length];

            for (int i = 0; i < columnarfile.columnsInfo.length; i++) {
                result[i] = columnarfile.columnsInfo[i].columnNo;
            }

            return result;
        }

        return getColumnsNo(columnarfile, targetColumnNames);
    }
}
