package programs;

import java.io.IOException;
import java.util.ArrayList;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import global.AttrOperator;
import global.AttrType;
import global.Convert;
import global.IndexType;
import global.RID;
import global.SystemDefs;
import global.TID;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import index.IndexScan;
import iterator.CondExpr;
import iterator.FldSpec;
import iterator.RelSpec;

class ValueConstraint<T> {
    public String columnName;
    public String operator;
    public T value;

    public ValueConstraint(String columnName, String operator, T value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
    }
}


public class Query {
    public static void main(String[] args) {
        if (!isValidInput(args)) {
            System.out.println(
                    "query [:COLUMNDBNAME] [:COLUMNARFILENAME] [:TARGETCOLUMNNAMES] [:VALUECONSTRAINT] [:NUMBUF] [:ACCESSTYPE]");
            return;
        }

        String[] targetColumnNames = new String[0];
        if (args.length > 5) {
            targetColumnNames = args[3].split(",");
        }

        ValueConstraint valueConstraint;

        if (java.util.regex.Pattern.matches("\\d+", args[args.length - 4])) {
            valueConstraint = new ValueConstraint<Integer>(args[args.length - 6],
                    args[args.length - 5], Integer.parseInt(args[args.length - 4]));
        } else {
            valueConstraint = new ValueConstraint<String>(args[args.length - 6],
                    args[args.length - 5], args[args.length - 4]);
        }
        int numBuf = Integer.parseInt(args[7]);

        execute(args[1], args[2], targetColumnNames, valueConstraint, numBuf, args[8]);

        return;
    }

    private static boolean isValidInput(String[] args) {
        return args.length == 9 && args[0].equals("query");
    }

    public static boolean execute(String columnDBName, String columnarFileName,
            String[] targetColumnNames, ValueConstraint valueConstraint, int numBuf,
            String accessType)
            throws IOException, HFException, HFBufMgrException, HFDiskMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        new SystemDefs(columnDBName, 100, numBuf, null);
        Tuple[] scanResult = new Tuple[0];
        boolean needSelect = true;

        switch (accessType) {
            case "FILESCAN":
                scanResult = doFileScan(columnarFileName, valueConstraint);
                break;

            case "COLUMNSCAN":
                scanResult = doColumnScan(columnarFileName, valueConstraint);
                break;

            case "BTREE":
                scanResult = doBtreeScan(columnarFileName, valueConstraint);
                targetColumnNames = new String[] {valueConstraint.columnName};
                needSelect = false;
                break;

            case "BITMAP":
                scanResult = doBitMapScan(columnarFileName, valueConstraint);
                break;

            default:
                break;
        }

        printResult(scanResult, columnarFileName, targetColumnNames, needSelect);

        return true;
    }

    private static Tuple[] doFileScan(String columnarFileName, ValueConstraint valueConstraint) {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        AttrType[] attrTypes = getInputAttrTypes(columnarFile.columnsInfo);
        short[] stringSizes = getStringSizes(columnarFile.columnsInfo);
        FldSpec[] proj_list = getProjList(columnarFile);
        CondExpr[] outFilter = getOutFilter(columnarFile, valueConstraint);
        ColumnarFileScan scanner = new ColumnarFileScan(columnarFileName, attrTypes, stringSizes,
                columnarFile.columnsInfo.length, columnarFile.columnsInfo.length, proj_list,
                outFilter);

        ArrayList<Tuple> result = new ArrayList<Tuple>();
        Tuple curResult;
        while ((curResult = scanner.get_next()) != null) {
            result.add(curResult);
        }

        return result.toArray(new Tuple[0]);
    }

    private static AttrType[] getInputAttrTypes(ColumnInfo[] columnsInfo) {
        AttrType[] attrTypes = new AttrType[columnsInfo.length];

        for (int i = 0; i < attrTypes.length; i++) {
            attrTypes[i] = columnsInfo[i].type;
        }

        return attrTypes;
    }

    private static short[] getStringSizes(ColumnInfo[] columnsInfo) {
        int stringCount = 0;
        for (ColumnInfo columnInfo : columnsInfo) {
            if (columnInfo.type.attrType == AttrType.attrString) {
                stringCount++;
            }
        }

        short[] stringSizes = new short[stringCount];

        for (int i = 0; i < stringCount; i++) {
            if (columnsInfo[i].type.attrType == AttrType.attrString) {
                stringSizes[i] = (short) columnsInfo[i].sizeInBytes;
            }
        }

        return stringSizes;
    }

    private static FldSpec[] getProjList(Columnarfile columnarFile) {
        FldSpec[] proj_list = new FldSpec[columnarFile.columnsInfo.length];
        for (int i = 0, offset = 0; i < columnarFile.columnsInfo.length; i++) {
            RelSpec relSpec = new RelSpec(0);
            proj_list[i] = new FldSpec(relSpec, offset);
            offset += columnarFile.columnsInfo[i].sizeInBytes;
        }

        return proj_list;
    }

    private static CondExpr[] getOutFilter(Columnarfile columnarFile,
            ValueConstraint valueConstraint) {
        int constraintColumnNo =
                getTargetColumnNos(columnarFile, new String[] {valueConstraint.columnName})[0];

        CondExpr outFilter = new CondExpr();
        outFilter.type1 = columnarFile.columnsInfo[constraintColumnNo].type;
        outFilter.type2 = columnarFile.columnsInfo[constraintColumnNo].type;

        outFilter.op = new AttrOperator(valueConstraint.operator);

        return new CondExpr[] {outFilter};
    }

    private static Tuple[] doColumnScan(String columnarFileName, ValueConstraint valueConstraint) {
        ArrayList<Tuple> scanResult = new ArrayList<Tuple>();
        Tuple compared;
        Tuple tidTuple;
        RID redundentRID = new RID();
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int constraintColumnNo =
                getTargetColumnNos(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo constraintColumnInfo = columnarFile.columnsInfo[constraintColumnNo];

        Scan columnScanner = columnarFile.columns[constraintColumnNo].openScan();
        Scan tidScanner = columnarFile.tidHeap.openScan();
        while ((compared = columnScanner.getNext(redundentRID)) != null) {
            tidTuple = tidScanner.getNext(redundentRID);

            boolean compareResult = false;
            if (constraintColumnInfo.type.attrType == 1) {
                int value = Convert.getIntValue(0, tidTuple.getTupleByteArray());
                compareResult =
                        compareInt(value, valueConstraint.operator, (int) valueConstraint.value);
            } else {
                String value = Convert.getStrValue(0, tidTuple.getTupleByteArray(),
                        constraintColumnInfo.sizeInBytes);
                compareResult = compareString(value, valueConstraint.operator,
                        (String) valueConstraint.value);
            }

            if (compareResult) {
                TID tid = new TID(0, tidTuple.getTupleByteArray());
                Tuple rowTuple = columnarFile.getTuple(tid);
                scanResult.add(rowTuple);
            }
        }

        return scanResult.toArray(new Tuple[0]);
    }

    private static int[] getTargetColumnNos(Columnarfile columnarFile, String[] columnNames) {
        int[] columnNos = new int[columnNames.length];

        for (int i = 0; i < columnNames.length; i++) {
            columnNos[i] = columnarFile.getColumnNoFrom(columnNames[i]);
        }

        return columnNos;
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
                return val1 == val2;

            default:
                return false;
        }
    }

    private static Tuple[] doBtreeScan(String columnarFileName, ValueConstraint valueConstraint) {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int constraintColumnNo =
                getTargetColumnNos(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo columnInfo = columnarFile.columnsInfo[constraintColumnNo];
        IndexType indexType = new IndexType(1);
        String indexName = columnarFile.getBtreeFileName(constraintColumnNo);
        AttrType[] types = new AttrType[] {columnInfo.type};
        short[] stringSizes = new short[1];
        if (columnInfo.type.attrType == AttrType.attrString) {
            stringSizes[0] = (short) columnInfo.sizeInBytes;
        } else {
            stringSizes = new short[0];
        }
        RelSpec relSpec = new RelSpec(0);
        FldSpec[] outFlds = new FldSpec[] {new FldSpec(relSpec, 0)};
        CondExpr[] selects = getOutFilter(columnarFile, valueConstraint);

        IndexScan scanner = new IndexScan(indexType, indexName + "-Btree-scanner", indexName, types,
                stringSizes, 1, 1, outFlds, selects, 1, true);

        ArrayList<Tuple> result = new ArrayList<Tuple>();
        Tuple curResult;
        while ((curResult = scanner.get_next()) != null) {
            result.add(curResult);
        }

        return result.toArray(new Tuple[0]);
    }

    private static Tuple[] doBitMapScan(String columnarFileName, ValueConstraint valueConstraint) {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int constraintColumnNo =
                getTargetColumnNos(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo columnInfo = columnarFile.columnsInfo[constraintColumnNo];
        IndexType indexType = new IndexType(3);
        String indexName = columnarFile.getBitMapFileName(constraintColumnNo);
        AttrType[] types = new AttrType[] {columnInfo.type};
        short[] stringSizes = new short[1];
        if (columnInfo.type.attrType == AttrType.attrString) {
            stringSizes[0] = (short) columnInfo.sizeInBytes;
        } else {
            stringSizes = new short[0];
        }
        RelSpec relSpec = new RelSpec(0);
        FldSpec[] outFlds = new FldSpec[] {new FldSpec(relSpec, 0)};
        CondExpr[] selects = getOutFilter(columnarFile, valueConstraint);

        ColumnarIndexScan scanner = new ColumnarIndexScan(indexType, indexName + "Bitmap--scanner",
                indexName, types, stringSizes, 1, 1, outFlds, selects, 1, true);

        ArrayList<Tuple> result = new ArrayList<Tuple>();
        Tuple curResult;
        while ((curResult = scanner.get_next()) != null) {
            result.add(curResult);
        }

        return result.toArray(new Tuple[0]);
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
}
