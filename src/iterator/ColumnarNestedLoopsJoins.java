package iterator;

import columnar.Columnarfile;
import global.AttrType;
import global.Convert;
import global.RID;
import heap.*;
import index.IndexException;
import programs.ScanType;
import global.TID;

import java.io.IOException;

import bufmgr.PageNotReadException;

class ScannerInterface {
    Columnarfile columnarfile;
    ScanType scanType;
    int columnNo;
    ColumnarFileScan columnarScanner;
    Scan columnScanner;
    Scan tidScanner;
    Tuple rowTuple;
    TID tid;

    public ScannerInterface(ScanType scanType, Columnarfile columnarfile, int columnNo)
            throws FileScanException, TupleUtilsException, InvalidRelation, IOException,
            InvalidTupleSizeException {
        this.columnarfile = columnarfile;
        this.scanType = scanType;
        this.columnNo = columnNo;
        this.setScanner();
    }

    public void setScanner() throws FileScanException, TupleUtilsException, InvalidRelation,
            IOException, InvalidTupleSizeException {
        if (this.scanType.scanType == ScanType.File) {
            this.columnarScanner = new ColumnarFileScan(this.columnarfile.name);
        } else {
            this.columnScanner = this.columnarfile.openColumnScan(this.columnNo);
            this.tidScanner = columnarfile.tidHeap.openScan();
        }
    }

    public Tuple getNext() throws JoinsException, InvalidTupleSizeException, InvalidTypeException,
            PageNotReadException, PredEvalException, UnknowAttrType, FieldNumberOutOfBoundException,
            WrongPermat, IOException {
        if (this.scanType.scanType == ScanType.File) {
            this.rowTuple = this.columnarScanner.get_next(new TID());
        } else {
            if ((this.rowTuple = this.columnScanner.getNext(new RID())) == null) {
                return null;
            }
            Tuple tidTuple = this.tidScanner.getNext(new RID());
            this.tid = new TID(0, tidTuple.getTupleByteArray());
        }

        return this.rowTuple;
    }

    public Tuple getRowTuple() {
        if (this.scanType.scanType == ScanType.File) {
            return this.rowTuple;
        } else {
            return this.columnarfile.getTuple(this.tid);
        }
    }

    public int comparedTupleAt() {
        int position = 0;
        if (this.scanType.scanType == ScanType.File) {
            for (int i = 0; i < this.columnNo; i++) {
                position += this.columnarfile.columnsInfo[i].sizeInBytes;
            }
        }

        return position;
    }

    public void reset() throws FileScanException, TupleUtilsException, InvalidRelation, IOException,
            InvalidTupleSizeException {
        this.close();
        this.setScanner();
    }

    public void close() {
        if (this.scanType.scanType == ScanType.File) {
            this.columnarScanner.close();
        } else {
            this.columnScanner.closescan();
            this.tidScanner.closescan();
        }
    }
}


public class ColumnarNestedLoopsJoins extends Iterator {

    // variables
    public Columnarfile outerColumnarfile;
    public Scan outerColumnScanner;
    public Tuple outerColumnTuple;
    public Tuple outerTuple;
    public Scan outerTidScanner;
    public AttrType joinType;
    public Columnarfile innerColumnarfile;
    public ScannerInterface innerScanner;
    public int outerColumnIndex, innerColumnIndex;


    // Constructor
    public ColumnarNestedLoopsJoins(Columnarfile outerColumnarfile, int outerColumnIndex,
            AttrType joinType, Columnarfile innerColumnarfile, int innerColumnIndex,
            ScanType scanType)
            throws IOException, JoinsException, IndexException, InvalidTupleSizeException,
            InvalidTypeException, PageNotReadException, TupleUtilsException, PredEvalException,
            SortException, LowMemException, UnknowAttrType, UnknownKeyTypeException, Exception {

        this.outerColumnarfile = outerColumnarfile;
        this.outerColumnIndex = outerColumnIndex;
        this.outerColumnScanner = outerColumnarfile.openColumnScan(outerColumnIndex);
        this.outerTidScanner = outerColumnarfile.tidHeap.openScan();
        this.get_outer_next();
        this.joinType = joinType;
        this.innerColumnarfile = innerColumnarfile;
        this.innerColumnIndex = innerColumnIndex;
        this.innerScanner = new ScannerInterface(scanType, innerColumnarfile, innerColumnIndex);
    }

    public Tuple get_next()
            throws IOException, JoinsException, IndexException, InvalidTupleSizeException,
            InvalidTypeException, PageNotReadException, TupleUtilsException, PredEvalException,
            SortException, LowMemException, UnknowAttrType, UnknownKeyTypeException, Exception {
        if (this.outerColumnTuple == null) {
            return null;
        }

        Tuple innerTuple;
        while ((innerTuple = this.innerScanner.getNext()) != null) {
            byte[] outerByte = this.outerColumnTuple.getTupleByteArray();
            byte[] innerByte = innerTuple.getTupleByteArray();
            boolean needJoin = false;
            if (joinType.attrType == AttrType.attrInteger) {
                int outerIntValue = Convert.getIntValue(0, outerByte);
                int innerIntValue =
                        Convert.getIntValue(this.innerScanner.comparedTupleAt(), innerByte);
                if (outerIntValue == innerIntValue) {
                    needJoin = true;
                }
            } else if (joinType.attrType == AttrType.attrString) {
                String outerStrValue = Convert.getStrValue(0, outerByte,
                        outerColumnarfile.columnsInfo[outerColumnIndex].sizeInBytes);
                String innerStrValue = Convert.getStrValue(this.innerScanner.comparedTupleAt(),
                        innerByte, innerColumnarfile.columnsInfo[innerColumnIndex].sizeInBytes);
                if (outerStrValue.equals(innerStrValue)) {
                    needJoin = true;
                }
            }

            if (needJoin) {
                Tuple joinInnerTuple = this.innerScanner.getRowTuple();
                byte[] joinedByte = Tuple.concateByte(this.outerTuple, joinInnerTuple);

                return new Tuple(joinedByte, 0, joinedByte.length);
            }
        }

        this.innerScanner.reset();
        this.get_outer_next();

        return this.get_next();
    }

    private void get_outer_next() throws InvalidTupleSizeException, IOException {
        this.outerColumnTuple = this.outerColumnScanner.getNext(new RID());
        Tuple outerTidTuple = this.outerTidScanner.getNext(new RID());
        if (outerTidTuple != null) {
            TID outerTid = new TID(0, outerTidTuple.getTupleByteArray());
            this.outerTuple = outerColumnarfile.getTuple(outerTid);
        }
    }

    // close function to finish joining
    public void close() {
        if (outerColumnScanner != null) {
            outerColumnScanner.closescan();
        }
        if (outerTidScanner != null) {
            outerTidScanner.closescan();
        }
        this.innerScanner.close();
    }
}
