package programs;

import java.util.ArrayList;
import global.RID;
import global.SystemDefs;
import heap.Scan;
import heap.Tuple;

class ValueConstraint {
    public String columnName;
    public String operator;
    public int value;

    public ValueConstraint(String columnName, String operator, int value) {
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

        String[] targetColumnNames = args[3].split(",");
        ValueConstraint valueConstraint =
                new ValueConstraint(args[4], args[5], Integer.parseInt(args[6]));
        int numBuf = Integer.parseInt(args[7]);

        execute(args[1], args[2], targetColumnNames, valueConstraint, numBuf, args[8]);

        return;
    }

    private static boolean isValidInput(String[] args) {
        return args.length == 9 && args[0].equals("query")
                && java.util.regex.Pattern.matches("\\d+", args[6])
                && java.util.regex.Pattern.matches("\\d+", args[7]);
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
        RID redundent = new RID();
        ColumnarFile columnarFile = new ColumnarFile(columnarFileName);
        int constraintColumnNo =
                getTargetColumnNo(columnarFile, new String[] {valueConstraint.columnName})[0];
        Scan scanner = columnarFile.openColumnScan(constraintColumnNo);
        while ((compared = scanner.getNext(redundent)) != null) {

        }
    }
}
