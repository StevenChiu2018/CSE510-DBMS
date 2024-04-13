package programs;

import java.io.IOException;
import java.util.HashSet;
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
import global.AttrType;
import global.ByteValue;
import global.Convert;
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
import index.IndexUtils;


public class ProgramIndex {
    public static void main(String[] args)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException, HashOperationException, PageUnpinnedException, PagePinnedException,
            PageNotFoundException, BufMgrException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException, GetFileEntryException,
            ConstructPageException, AddFileEntryException, IteratorException, cbitmap.GetFileEntryException,
            cbitmap.ConstructPageException, cbitmap.UnpinPageException, cbitmap.AddFileEntryException,
            cbitmap.PinPageException, Exception {
        if (!isValidInput(args)) {
            System.out.println(
                    "index [:COLUMNDBNAME] [:COLUMNARFILENAME] [:COLUMNNAME] [:INDEXTYPE]");
            return;
        }

        try {
            execute(args[1], args[2], args[3], args[4]);
        } catch (GetFileEntryException e) {
            e.printStackTrace();
            return;
        }

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
            ConstructPageException, AddFileEntryException, IteratorException, cbitmap.GetFileEntryException,
            cbitmap.UnpinPageException, cbitmap.ConstructPageException, cbitmap.AddFileEntryException,
            cbitmap.PinPageException, Exception {
        new SystemDefs(columnDBName, 0, 100, null);

        boolean result = false;

        switch (IndexType) {
            case "Btree":
                result = useBtreeIndex(columnarFileName, columnName);
                break;

            case "BITMAP":
                result = useBitMapIndex(columnarFileName, columnName);
                break;

            // For testing compressed bitmap file, can be deleted in the future
            case "CBITMAP":
                result = useCBitMapIndex(columnarFileName, columnName);
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
        HashSet<String> insertedIndexKey = new HashSet<String>();
        Pcounter.initialize();
        while ((value = columnScan.getNext(rid)) != null) {
            byte[] trueValueByte = value.getTupleByteArray();
            String trueValueString = "";

            if (columnInfo.type.attrType == AttrType.attrInteger) {
                int intValue = Convert.getIntValue(0, trueValueByte);
                trueValueString = Integer.toString(intValue);
            } else {
                trueValueString = Convert.getStrValue(0, trueValueByte, columnInfo.sizeInBytes);
            }

            if (!insertedIndexKey.contains(trueValueString)) {
                ByteValue byteVaule = new ByteValue(trueValueByte, columnInfo.type.attrType,
                        columnInfo.sizeInBytes);
                columnarFile.createBitMapIndex(columnInfo.columnNo, byteVaule);
                insertedIndexKey.add(trueValueString);
            }

            System.out.print("The " + count++ + "th key is inserted to bitmap\r");
        }

        System.out.println(count + " keys are inserted to bitmap");
        System.out.println(Pcounter.usage_in_string());
        columnScan.closescan();
        return true;
    }

    /**
     *  For testing compressed bitmap file, can be deleted in the future
     * @param columnarFileName
     * @param columnName
     * @return
     * @throws HFDiskMgrException
     * @throws HFException
     * @throws HFBufMgrException
     * @throws InvalidTupleSizeException
     * @throws SpaceNotAvailableException
     * @throws InvalidSlotNumberException
     * @throws IOException
     * @throws UnpinPageException
     * @throws PinPageException
     * @throws PageUnpinnedException
     * @throws InvalidFrameNumberException
     * @throws HashEntryNotFoundException
     * @throws ReplacerException
     * @throws GetFileEntryException
     * @throws ConstructPageException
     * @throws AddFileEntryException
     * @throws IteratorException
     * @throws cbitmap.GetFileEntryException
     * @throws cbitmap.UnpinPageException
     * @throws cbitmap.ConstructPageException
     * @throws cbitmap.AddFileEntryException
     * @throws cbitmap.PinPageException
     * @throws Exception
     */
    private static boolean useCBitMapIndex(String columnarFileName, String columnName)
            throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
            SpaceNotAvailableException, InvalidSlotNumberException, IOException, UnpinPageException,
            PinPageException, PageUnpinnedException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException, GetFileEntryException,
            ConstructPageException, AddFileEntryException, IteratorException, cbitmap.GetFileEntryException,
            cbitmap.UnpinPageException, cbitmap.ConstructPageException, cbitmap.AddFileEntryException,
            cbitmap.PinPageException, Exception {
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
            columnarFile.createCBitMapIndex(columnInfo.columnNo, byteVaule);
            count++;
            //System.out.print("The " + count++ + "th key is inserted to bitmap\r");
        }

        System.out.println(count + " keys are inserted to bitmap");
        System.out.println(Pcounter.usage_in_string());
        columnScan.closescan();
        return true;
    }
}
