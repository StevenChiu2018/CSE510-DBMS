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

  public Scan dataHeapFile;
  public Scan tidHeapFile;
  public Columnarfile columnarfile;

  public TupleScan(Columnarfile cf) {
    this.columnarfile = cf;
    dataHeapFile = cf.heapfiles[0].openScan();
    tidHeapFile = cf.tidHeapFile.openScan();

  }

  public void closetuplescan() {

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
      currentLength = columnarfile.heapfiles[i].getRecord(tid.recordIDs[i]).getLength();

      resultTuple.tupleSet(columnarfile.heapfiles[i].getRecord(tid.recordIDs[i]), currentOffset, currentLength);

      currentOffset += currentLength;

  
    }

    return resultTuple;

  }


  public boolean position(TID tid) {

  }

}

