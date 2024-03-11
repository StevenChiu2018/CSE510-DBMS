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
import ColumnarFile;


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

    TID tid = new TID(byteArray);

    Tuple resultTuple = new Tuple();
    int currentOffset = 0;

    // In this for loop, insert tuple into result tuple.
    for (int i = 0; i < tid.numRIDs; i++) {

      int currentLength;
      currentLength = columnarfile.columns[i].getRecord(tid.recordIDs[i]).getLength();
      resultTuple.tupleSet(columnarfile.columns[i].getRecord(tid.recordIDs[i]), currentOffset, currentLength);
      currentOffset += currentLength;
  
    }

    return resultTuple;

  }

  

  public boolean position(TID tid) {

    RID rid = new RID();
    Scan tidHeapFileForScan = columnarfile.heapfiles[0].openScan();
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




