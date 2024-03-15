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
import heap.Scan;
import heap.Tuple;


public class TupleScan {

  // tidHeapFile need to be created from columnarFile class.
  public Scan tidHeapFile;
  public Columnarfile columnarfile;

  public TupleScan(Columnarfile cf) {
    this.columnarfile = cf;
    tidHeapFile = cf.tidHeap.openScan();

  }

  public void closetuplescan() {
    tidHeapFile.closescan();
  }

  public Tuple getNext(TID tid) {

    RID rid = null;
    Tuple tuple;
    byte[] byteArray;


    // Traverse tidHeapFile
    tuple = tidHeapfile.getNext(rid);

    // byteArray stores target byteArray
    byteArray = tuple.getTupleByteArray();

    Tuple resultTuple = new Tuple();

    tid = new TID(0, byteArray);

    resultTuple = columnarfile.getTuple(tid);

    return resultTuple;

  }

  

  public boolean position(TID tid) {

    RID rid = new RID();
    Scan tidHeapFileForScan = columnarfile.columns[0].openScan();
    Tuple tupleForScan;

    while ((tupleForScan = tidHeapFileForScan.getNext(rid)) != null) {
      byteArrayForScan = tupleForScan.getTupleByteArray();
      tidForScan = new TID(byteArrayForScan);
      if (tidForScan.position == tid.position){
        tidHeapFile.closescan();
        tidHeapFile = tidHeapFileForScan;
        return true;
      }
    }
    return false
    
  }

}




