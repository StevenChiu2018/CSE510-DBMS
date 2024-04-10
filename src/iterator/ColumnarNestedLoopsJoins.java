package iterator;

import columnar.Columnarfile;
import columnar.TupleScan;
import global.AttrType;
import heap.*;
import index.IndexException;

import java.io.IOException;

public class ColumnarNestedLoopsJoins extends Iterator {
    
    // variables
    public Columnarfile outercColumnarfile;
    public Scan outerColumnarfileScanner;
    public Scan outerTidScanner;
    public AttrType outerType;
    public Columnarfile innColumnarfile;
    public Scan innerColumnarfileScanner;

    // Constructor
    public ColumnarNestedLoopsJoins(Columnarfile outerColumnarfile, Scan outerColumnarfileScanner, AttrType outerType, Columnarfile innerColumnarfile, Scan innerColumnScanner) {

        this.outerColumnarfile = outerColumnarfile;
        this.outerColumnarfileScanner = outerColumnarfile.openColumnScan();
        this.outerTidScanner = outerColumnarfile.tidHeap.openScan()
        this.outerType = outerType;
        this.innerColumnarfile = innerColumnarfile;
        this.innerColumnarfileScanner = innerColumnarfileScanner;

    }

    // get next function
    public Tuple get_next() {
        
    }

    // close function to finish joining
    public void close() {
        if (!closeFlag) {
            closeFlag = true;
        }
    }
}
