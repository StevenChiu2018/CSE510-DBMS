package columnar;

/** JAVA */
/**
 * TupleScan.java- class Scan
 *
 */

import java.io.*;
import global.*;
import global.TID;
import global.AttrType;
import heap.Heapfile;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.Tuple;


public class TupleScan {

  // tidHeapFile need to be created from columnarFile class.
  public Scan tidHeapScanner;
  public Columnarfile columnarfile;

  public TupleScan(Columnarfile cf) throws InvalidTupleSizeException, IOException {
    this.columnarfile = cf;
    tidHeapScanner = cf.tidHeap.openScan();

  }

  public void closetuplescan() {
    tidHeapScanner.closescan();
  }

  public Tuple getNext(TID tid) throws IOException, InvalidTupleSizeException {

    RID rid = null;
    Tuple tuple;
    byte[] byteArray;


    // Traverse tidHeapFile
    tuple = tidHeapScanner.getNext(rid);

    // byteArray stores target byteArray
    byteArray = tuple.getTupleByteArray();

    Tuple resultTuple = new Tuple();

    tid = new TID(0, byteArray);

    resultTuple = columnarfile.getTuple(tid);

    return resultTuple;

  }



  public boolean position(TID tid) throws InvalidTupleSizeException, IOException {
    RID rid = new RID();
    Scan tidHeapFileForScan = columnarfile.columns[0].openScan();
    Tuple tidTuple;
    byte[] tidByte;

    while ((tidTuple = tidHeapFileForScan.getNext(rid)) != null) {
      tidByte = tidTuple.getTupleByteArray();
      TID curTid = new TID(0, tidByte);

      if (curTid.position == tid.position) {
        tidHeapScanner.closescan();
        tidHeapScanner = tidHeapFileForScan;
        return true;
      }
    }

    return false;
  }

  public void closescan() {
    this.tidHeapScanner.closescan();
  }
}


