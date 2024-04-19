package columnar;

/** JAVA */
/**
 * TupleScan.java- class Scan
 *
 */

import java.io.*;
import java.util.ArrayList;
import global.*;
import global.TID;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.Tuple;


public class TupleScan {

  // tidHeapFile need to be created from columnarFile class.
  public Columnarfile columnarfile;
  public ArrayList<Scan> columnScanners;
  public Scan tidScanner;

  public TupleScan(Columnarfile cf) throws InvalidTupleSizeException, IOException {
    this.columnarfile = cf;
    this.tidScanner = this.columnarfile.tidHeap.openScan();
    this.columnScanners = new ArrayList<Scan>();
    for (int i = 0; i < this.columnarfile.columns.length; i++) {
      this.columnScanners.add(this.columnarfile.columns[i].openScan());
    }
  }

  public void closetuplescan() {
    this.tidScanner.closescan();
    for (int i = 0; i < this.columnScanners.size(); i++) {
      this.columnScanners.get(i).closescan();
    }
  }

  public Tuple getNext(TID tid) throws IOException, InvalidTupleSizeException {
    Tuple result = new Tuple(new byte[0], 0, 0);
    RID rid = new RID();
    Tuple columnTuple;

    for (int i = 0; i < this.columnScanners.size(); i++) {
      if ((columnTuple = this.columnScanners.get(i).getNext(rid)) == null) {
        return null;
      }

      byte[] curAccuByte = Tuple.concateByte(result, columnTuple);
      result = new Tuple(curAccuByte, 0, curAccuByte.length);
    }

    Tuple tidTuple = this.tidScanner.getNext(new RID());
    TID curTID = new TID(0, tidTuple.getTupleByteArray());
    tid.copyTid(curTID);

    return result;
  }

  public void closescan() {
    this.closetuplescan();
  }
}


