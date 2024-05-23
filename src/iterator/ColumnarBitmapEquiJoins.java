package iterator;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import bitmap.BitMapFile;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import cbitmap.UnpinPageException;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import global.*;
import heap.*;
import index.*;


public class ColumnarBitmapEquiJoins extends Iterator {
    public Columnarfile columnarFileL;
    public ColumnInfo joinColumnInfoL;
    public Scan joinFieldScannerL;
    public Tuple tupleJoinFieldL;
    public Scan tidScannerL;
    public Tuple tupleL;
    public HashMap<String, ColumnarIndexScan> valueColumnIndexScanner =
            new HashMap<String, ColumnarIndexScan>();

    public ColumnarBitmapEquiJoins(Columnarfile columnarfileL, int leftJoinField,
            IndexType indexType, Columnarfile columnarfileR, int rightJoinField)
            throws InvalidTupleSizeException, IndexException, InvalidTypeException,
            UnknownIndexTypeException, UnknownKeyTypeException, UnknowAttrType,
            FieldNumberOutOfBoundException, IOException, HFDiskMgrException, GetFileEntryException,
            PinPageException, ConstructPageException, cbitmap.GetFileEntryException,
            cbitmap.PinPageException, cbitmap.ConstructPageException, HFException,
            HFBufMgrException, InvalidSlotNumberException, UnpinPageException,
            bitmap.UnpinPageException, PageUnpinnedException, InvalidFrameNumberException,
            HashEntryNotFoundException, ReplacerException {
        this.columnarFileL = columnarfileL;
        this.joinFieldScannerL = columnarfileL.openColumnScan(leftJoinField);
        this.joinColumnInfoL = columnarfileL.columnsInfo[leftJoinField];
        this.tidScannerL = columnarfileL.tidHeap.openScan();
        this.get_left_next();

        Scan columnScanner = columnarfileL.openColumnScan(leftJoinField);
        Tuple tupleL;
        RID rid = new RID();
        while ((tupleL = columnScanner.getNext(rid)) != null) {
            String bmf = "";
            String valueString = "";
            if (this.joinColumnInfoL.type.attrType == AttrType.attrInteger) {
                int intValue = Convert.getIntValue(0, tupleL.getTupleByteArray());
                bmf = this.get_bitmap_file_name_if_exist(columnarfileR, rightJoinField,
                        Integer.toString(intValue), indexType);
                valueString = Integer.toString(intValue);
            } else {
                String strValue = Convert.getStrValue(0, tupleL.getTupleByteArray(),
                        this.joinColumnInfoL.sizeInBytes);
                bmf = this.get_bitmap_file_name_if_exist(columnarfileR, rightJoinField, strValue,
                        indexType);
                valueString = strValue;
            }

            if (bmf == "") {
                continue;
            }

            if (!this.valueColumnIndexScanner.containsKey(valueString)) {
                IndexType[] indexTypes = new IndexType[] {indexType};
                String[] indexNames = new String[] {bmf};
                ColumnarIndexScan columnarIndexScanner =
                        new ColumnarIndexScan(columnarfileR.name, indexTypes, indexNames);
                this.valueColumnIndexScanner.put(valueString, columnarIndexScanner);
            }
        }
        columnScanner.closescan();
    }

    private String get_bitmap_file_name_if_exist(Columnarfile columnarfile, int columnNo,
            String strValue, IndexType indexType) throws HFDiskMgrException, GetFileEntryException {
        String name;
        if (indexType.indexType == IndexType.Bitmap) {
            name = columnarfile.getBitMapFileName(columnNo, strValue);
        } else {
            name = columnarfile.getCBitMapFileName(columnNo, strValue);
        }

        if (BitMapFile.get_file_entry(name) == null) {
            return "";
        }

        return name;
    }

    public Tuple get_next()
            throws IndexException, UnknownKeyTypeException, IOException, InvalidTupleSizeException {
        if (this.tupleJoinFieldL == null) {
            return null;
        }

        String valueString = "";
        if (this.joinColumnInfoL.type.attrType == AttrType.attrInteger) {
            int intValue = Convert.getIntValue(0, this.tupleJoinFieldL.getTupleByteArray());
            valueString = Integer.toString(intValue);
        } else {
            String strValue = Convert.getStrValue(0, this.tupleJoinFieldL.getTupleByteArray(),
                    this.joinColumnInfoL.sizeInBytes);
            valueString = strValue;
        }

        if (this.valueColumnIndexScanner.containsKey(valueString)) {
            ColumnarIndexScan indexScanner = this.valueColumnIndexScanner.get(valueString);
            Tuple tupleR = indexScanner.get_next();

            if (tupleR != null) {
                byte[] newByte = Tuple.concateByte(tupleL, tupleR);
                return new Tuple(newByte, 0, newByte.length);
            } else {
                indexScanner.resetScanner();
            }
        }

        this.get_left_next();

        return this.get_next();
    }

    private void get_left_next() throws InvalidTupleSizeException, IOException {
        this.tupleJoinFieldL = this.joinFieldScannerL.getNext(new RID());
        Tuple tupleTidL;
        tupleTidL = this.tidScannerL.getNext(new RID());
        if (tupleTidL != null) {
            TID tidL = new TID(0, tupleTidL.getTupleByteArray());
            this.tupleL = this.columnarFileL.getTuple(tidL);
        }
    }

    public void close() throws IOException, IndexException {
        if (!closeFlag) {
            this.joinFieldScannerL.closescan();
            this.tidScannerL.closescan();
            for (Map.Entry<String, ColumnarIndexScan> entry : this.valueColumnIndexScanner
                    .entrySet()) {
                entry.getValue().close();
            }
        }

        closeFlag = true;
    }
}
