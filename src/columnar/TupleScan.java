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
    reset();
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
  public Tuple getNext(TID tid) throws InvalidTupleSizeException, IOException {
    Tuple recptrtuple = null;

    if (nextUserStatus != true) {
      nextDataPage();
    }

    if (datapage == null)
      return null;

    rid.pageNo.pid = userrid.pageNo.pid;
    rid.slotNo = userrid.slotNo;

    try {
      recptrtuple = datapage.getRecord(rid);
    }

    catch (Exception e) {
      // System.err.println("SCAN: Error in Scan" + e);
      e.printStackTrace();
    }

    userrid = datapage.nextRecord(rid);
    if (userrid == null)
      nextUserStatus = false;
    else
      nextUserStatus = true;

    return recptrtuple;
  }


  /**
   * Position all scans cursors to the records with the given tid.
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   * @param tid Record ID of the given record
   * @return true if successful, false otherwise.
   */
  public boolean position(TID tid) throws InvalidTupleSizeException, IOException {
    RID nxtrid = new RID();
    boolean bst;

    bst = peekNext(nxtrid);

    if (nxtrid.equals(rid) == true)
      return true;

    // This is kind lame, but otherwise it will take all day.
    PageId pgid = new PageId();
    pgid.pid = rid.pageNo.pid;

    if (!datapageId.equals(pgid)) {

      // reset everything and start over from the beginning
      reset();

      bst = firstDataPage();

      if (bst != true)
        return bst;

      while (!datapageId.equals(pgid)) {
        bst = nextDataPage();
        if (bst != true)
          return bst;
      }
    }

    // Now we are on the correct page.

    try {
      userrid = datapage.firstRecord();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (userrid == null) {
      bst = false;
      return bst;
    }

    bst = peekNext(nxtrid);

    while ((bst == true) && (nxtrid != rid))
      bst = mvNext(nxtrid);

    return bst;
  }

}

