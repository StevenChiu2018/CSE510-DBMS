package programs;

import java.io.IOException;
import bitmap.PinPageException;
import bitmap.UnpinPageException;
import columnar.Columnarfile;
import global.ByteValue;
import global.RID;
import global.SystemDefs;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;

public class index {
    public static void main(String[] args) throws HFDiskMgrException, HFException,
            HFBufMgrException, InvalidTupleSizeException, SpaceNotAvailableException,
            InvalidSlotNumberException, IOException, UnpinPageException, PinPageException {
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
            String IndexType) throws HFDiskMgrException, HFException, HFBufMgrException,
            InvalidTupleSizeException, SpaceNotAvailableException, InvalidSlotNumberException,
            IOException, UnpinPageException, PinPageException {
        new SystemDefs(columnDBName, 0, 100, null);

        switch (IndexType) {
            case "Btree":
                return useBtreeIndex(columnarFileName, columnName);

            case "BITMAP":
                return useBitMapIndex(columnarFileName, columnName);

            default:
                break;
        }

        return false;
    }

    private static boolean useBtreeIndex(String columnarFileName, String columnName)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int columnNo = columnarFile.getColumnNoFrom(columnName);

        return columnarFile.createBTreeIndex(columnNo);
    }

    private static boolean useBitMapIndex(String columnarFileName, String columnName)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        int columnNo = columnarFile.getColumnNoFrom(columnName);
        Scan columnScan = columnarFile.columns[columnNo].openScan();
        Tuple value;
        RID rid = new RID();

        while ((value = columnScan.getNext(rid)) != null) {
            ByteValue byteVaule = new ByteValue(value.getTupleByteArray());
            columnarFile.createBitMapIndex(columnNo, byteVaule);
        }

        return true;
    }
}
