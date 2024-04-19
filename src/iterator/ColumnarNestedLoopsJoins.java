package iterator;

import columnar.Columnarfile;
import columnar.TupleScan;
import global.AttrType;
import global.Convert;
import global.IntegerValue;
import global.RID;
import global.StringValue;
import global.ValueClass;
import heap.*;
import index.IndexException;
import global.TID;

import java.io.IOException;

import bufmgr.PageNotReadException;

public class ColumnarNestedLoopsJoins extends Iterator {

    // variables
    public Columnarfile outerColumnarfile;
    public Scan outerColumnScanner;
    public Scan outerTidScanner;
    public AttrType joinType;
    public Columnarfile innerColumnarfile;
    public Scan innerColumnScanner;
    public Scan innerTidScanner;
    public int outerColumnIndex, innerColumnIndex;


    // Constructor
    public ColumnarNestedLoopsJoins(Columnarfile outerColumnarfile, int outerColumnIndex,
            AttrType joinType, Columnarfile innerColumnarfile, int innerColumnIndex)
            throws IOException, JoinsException, IndexException, InvalidTupleSizeException,
            InvalidTypeException, PageNotReadException, TupleUtilsException, PredEvalException,
            SortException, LowMemException, UnknowAttrType, UnknownKeyTypeException, Exception {

        this.outerColumnarfile = outerColumnarfile;
        this.outerColumnIndex = outerColumnIndex;
        this.outerColumnScanner = outerColumnarfile.openColumnScan(outerColumnIndex);
        this.outerTidScanner = outerColumnarfile.tidHeap.openScan();
        this.joinType = joinType;
        this.innerColumnarfile = innerColumnarfile;
        this.innerColumnIndex = innerColumnIndex;
        this.innerColumnScanner = innerColumnarfile.openColumnScan(innerColumnIndex);
        this.innerTidScanner = innerColumnarfile.tidHeap.openScan();
    }

    // get next function
    public Tuple get_next()
            throws IOException, JoinsException, IndexException, InvalidTupleSizeException,
            InvalidTypeException, PageNotReadException, TupleUtilsException, PredEvalException,
            SortException, LowMemException, UnknowAttrType, UnknownKeyTypeException, Exception {

        RID outerRid = new RID();
        RID innerRid = new RID();

        // iterate outer relation using outer scanner
        Tuple outerTuple;
        while ((outerTuple = outerColumnScanner.getNext(outerRid)) != null) {

            Tuple outerTidTuple = outerTidScanner.getNext(outerRid);

            // iterate inner relation
            Tuple innerTuple;
            this.innerColumnScanner.closescan();
            this.innerColumnScanner = this.innerColumnarfile.openColumnScan(this.innerColumnIndex);
            this.innerTidScanner.closescan();
            this.innerTidScanner = this.innerColumnarfile.tidHeap.openScan();
            while ((innerTuple = innerColumnScanner.getNext(innerRid)) != null) {

                Tuple innerTidTuple = innerTidScanner.getNext(innerRid);

                // Compare outer tuple and inner tuple based on their type
                byte[] outerByte = outerTuple.getTupleByteArray();
                byte[] innerByte = innerTuple.getTupleByteArray();
                boolean needJoin = false;
                if (joinType.attrType == AttrType.attrInteger) {
                    int outerIntValue = Convert.getIntValue(0, outerByte);
                    int innerIntValue = Convert.getIntValue(0, innerByte);
                    if (outerIntValue == innerIntValue) {
                        // join
                        needJoin = true;
                    }
                } else if (joinType.attrType == AttrType.attrString) {
                    String outerStrValue = Convert.getStrValue(0, outerByte,
                            outerColumnarfile.columnsInfo[outerColumnIndex].sizeInBytes);
                    String innerStrValue = Convert.getStrValue(0, innerByte,
                            innerColumnarfile.columnsInfo[innerColumnIndex].sizeInBytes);
                    if (outerStrValue.equals(innerStrValue)) {
                        // join
                        needJoin = true;
                    }
                }

                if (needJoin) {
                    // join
                    TID outerTid = new TID(0, outerTidTuple.getTupleByteArray());
                    TID innerTid = new TID(0, innerTidTuple.getTupleByteArray());
                    Tuple joinOuterTuple = outerColumnarfile.getTuple(outerTid);
                    Tuple joinInnerTuple = innerColumnarfile.getTuple(innerTid);

                    byte[] joinedByte = Tuple.concateByte(joinOuterTuple, joinInnerTuple);
                    Tuple joinedTuple = new Tuple(joinedByte, 0, joinedByte.length);

                    return joinedTuple;
                }
            }

        }
        return null;
    }

    // close function to finish joining
    public void close() {
        if (outerColumnScanner != null) {
            outerColumnScanner.closescan();
        }
        if (innerColumnScanner != null) {
            innerColumnScanner.closescan();
        }
        if (outerTidScanner != null) {
            outerTidScanner.closescan();
        }
        if (innerTidScanner != null) {
            innerTidScanner.closescan();
        }
    }
}
