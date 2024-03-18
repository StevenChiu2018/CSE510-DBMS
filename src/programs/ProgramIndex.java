package programs;

import java.io.IOException;
import bitmap.AddFileEntryException;
import bitmap.BitMapFile;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bitmap.UnpinPageException;
import btree.IteratorException;
import bufmgr.BufMgrException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.HashOperationException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotFoundException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import diskmgr.Pcounter;
import global.ByteValue;
import global.Convert;
import global.RID;
import global.SystemDefs;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.Heapfile;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import index.ColumnIndexScan;

public class ProgramIndex {
    public static void main(String[] args)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException, HashOperationException, PageUnpinnedException, PagePinnedException,
            PageNotFoundException, BufMgrException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException, GetFileEntryException,
            ConstructPageException, AddFileEntryException, IteratorException {
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
            String IndexType)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException, HashOperationException, PageUnpinnedException, PagePinnedException,
            PageNotFoundException, BufMgrException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException, GetFileEntryException,
            ConstructPageException, AddFileEntryException, IteratorException {
        new SystemDefs(columnDBName, 0, 100, null);

        boolean result = false;

        switch (IndexType) {
            case "Btree":
                result = useBtreeIndex(columnarFileName, columnName);
                break;

            case "BITMAP":
                result = useBitMapIndex(columnarFileName, columnName);
                break;

            default:
                break;
        }

        SystemDefs.JavabaseBM.flushAllPages();
        SystemDefs.JavabaseDB.closeDB();

        return result;
    }

    private static boolean useBtreeIndex(String columnarFileName, String columnName)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        ColumnInfo columnInfo = columnarFile.getColumnInfoByColumnName(columnName);

        return columnarFile.createBTreeIndex(columnInfo.columnNo);
    }

    private static boolean useBitMapIndex(String columnarFileName, String columnName)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException, PageUnpinnedException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException, GetFileEntryException,
            ConstructPageException, AddFileEntryException, IteratorException {
        Columnarfile columnarFile = new Columnarfile(columnarFileName);
        ColumnInfo columnInfo = columnarFile.getColumnInfoByColumnName(columnName);
        Scan columnScan = columnarFile.columns[columnInfo.columnNo].openScan();
        Tuple value;
        RID rid = new RID();

        int count = 0;
        Pcounter.initialize();
        while ((value = columnScan.getNext(rid)) != null) {
            ByteValue byteVaule = new ByteValue(value.getTupleByteArray(), columnInfo.type.attrType,
                    columnInfo.sizeInBytes);
            columnarFile.createBitMapIndex(columnInfo.columnNo, byteVaule);
            System.out.print("The " + count++ + "th key is inserted to bitmap\r");
        }

        System.out.println(count + " keys are inserted to bitmap");
        System.out.println(Pcounter.usage_in_string());
        columnScan.closescan();
        return true;
    }
}
