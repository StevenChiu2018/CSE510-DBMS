package columnar;

/** JAVA */
/**
 * TupleScan.java- class Scan
 *
 */

import java.io.*;
import global.*;
import global.TID;
import heap.Heapfile;
import heap.Scan;
import heap.Tuple;


public class TupleScan implements GlobalConst {


  public Scan[] scan;
  public ColumnarFIle cf;

  /**
   * The constructor pins the first directory page in the file and initializes its private data
   * members from the private data member from cf
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   *
   * @param cf A ColumnarFile object
   */
  public TupleScan(Columnarfile cf) {

    int i = 0;
    this.cf = cf;
    this.scan = new Scan[cf.numColumns];

    try {
      for (Heapfile hf: cf.heapFileColumns) {
        scan[i] = hf.openScan();
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /** Closes the TupleScan object */
  public void closetuplescan() {

    for (Scan s: scan) {
      s.closescan();
    }

    scan = null;
  }

  /**
   * Retrieve the next tuple in a sequential scan
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   *
   * @param tid Tuple ID of the record
   * @return the Tuple of the retrieved tuple.
   */
  public Tuple getNext(TID tid) {


  }


  /**
   * Position all scans cursors to the records with the given tid.
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   * @param tid Record ID of the given record
   * @return true if successful, false otherwise.
   */
  public boolean position(TID tid) {

    int i = 0;

    try {
      for (Scan s: this.scan) {
        if(!s.position(tid.recordIDs[i])) {
          i++;
        }
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;

  }

}

