package programs;

import java.util.ArrayList;
import global.AttrType;
import global.Convert;
import global.RID;
import global.SystemDefs;
import global.TID;
import heap.Scan;
import heap.Tuple;

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
            String accessType) {
        new SystemDefs(columnDBName, 100, numBuf, null);
        Tuple[] scanResult;

        switch (accessType) {
            case "FILESCAN":
                scanResult = doFileSCAN(columnarFileName, targetColumnNames, valueConstraint);
                break;

            case "COLUMNSCAN":
                scanResult = doColumnScan(columnarFileName, targetColumnNames, valueConstraint);
                break;

            case "BTREE":
                scanResult = doBtreeScan(columnarFileName, targetColumnNames, valueConstraint);
                break;

            case "BITMAP":
                scanResult = doBitMapScan(columnarFileName, targetColumnNames, valueConstraint);
                break;

            default:
                break;
        }

        printResult(scanResult);

        return true;
    }

    private static Tuple[] doColumnScan(String columnarFileName, String[] targetColumnName,
            ValueConstraint valueConstraint) {
        ArrayList<Tuple> scanResult = new ArrayList<Tuple>();
        Tuple compared;
        Tuple tidTuple;
        RID redundentRID = new RID();
        ColumnarFile columnarFile = new ColumnarFile(columnarFileName);
        int constraintColumnNo =
                getTargetColumnNos(columnarFile, new String[] {valueConstraint.columnName})[0];
        ColumnInfo constraintColumnInfo = columnarFile.columns[constraintColumnNo];

        Scan columnScanner = columnarFile.columns[constraintColumnNo].openScan();
        Scan tidScanner = columnarFile.tidHeap.openScan();
        while ((compared = columnScanner.getNext(redundentRID)) != null) {
            tidTuple = tidScanner.getNext(redundentRID);

            boolean compareResult = false;
            if (constraintColumnInfo.type.attrType == 1) {
                int value = Convert.getIntValue(0, tid.getTupleByteArray());
                compareResult =
                        compareInt(value, valueConstraint.operator, (int) valueConstraint.value);
            } else {
                String value = Convert.getStrValue(0, tid.getTupleByteArray(),
                        constraintColumnInfo.sizeIntByte);
                compareResult = compareString(value, valueConstraint.operator,
                        (String) valueConstraint.value);
            }

            if (compareResult) {
                TID tid = new TID(tidTuple.getTupleByteArray());
                Tuple rowTuple = columnarFile.getTuple(tid);
                scanResult.add(rowTuple);
            }
        }

        return scanResult.toArray(new Tuple[0]);
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
            case "==":
                return val1 == val2;

            default:
                return false;
        }
    }
}
