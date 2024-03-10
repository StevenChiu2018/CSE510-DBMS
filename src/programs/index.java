package programs;

import global.RID;
import global.SystemDefs;
import heap.Scan;
import heap.Tuple;

public class index {
    public static void main(String[] args) {
        if (!isValidInput(args)) {
            System.out.println(
                    "index [:COLUMNDBNAME] [:COLUMNARFILENAME] [:COLUMNNAME] [:INDEXTYPE]");
            return;
        }

        execute(args[1], args[2], args[3], args[4]);

        return;
    }

    private static boolean isValidInput(String[] args) {
        return args.length == 5 && args[0].equals("index");
    }

    public static boolean execute(String columnDBName, String columnarFileName, String columnName,
            String IndexType) {
        new SystemDefs(columnDBName, 100, 100, null);

        switch (IndexType) {
            case "Btree":
                return useBtreeIndex(columnarFileName, columnName);
                break;

            case "BITMAP":
                return useBitMapIndex(columnarFileName, columnName);
                break;

            default:
                break;
        }
    }

    private static boolean useBtreeIndex(String columnarFileName, String columnName) {
        ColumnarFile columnarFile = new ColumnarFile(columnarFileName);
        int columnNo = columnarFile.getColumnNoFrom(columnName);

        return columnarFile.createBTreeIndex(columnNo);
    }

    private static boolean useBitMapIndex(String columnarFileName, String columnName) {
        ColumnarFile columnarFile = new ColumnarFile(columnarFileName);
        int columnNo = columnarFile.getColumnNoFrom(columnName);
        Scan columnScan = columnarFile.openScan();
        Tuple value;
        RID rid = new RID();

        while ((value = columnScan.getNext(rid)) != null) {
            ByteValue byteVaule = new ByteVaule(value.getTupleByteArray());
            columnarFile.createBitMapIndex(columnNo, byteVaule);
        }

        return true;
    }
}
