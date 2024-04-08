package cbitmap;

import java.io.IOException;
import btree.IteratorException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import diskmgr.*;
import global.*;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.HFPage;
import heap.Heapfile;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.Tuple;

public class CBM implements GlobalConst {
  public CBM() {};

  /**
   * For debug. Print the Bit map structure out
   *
   * @param header the head page of the Bit Map file
   * @exception IOException error from the lower layer
   * @exception ConstructPageException error from BM page constructor
   * @exception IteratorException error from iterator
   * @exception HashEntryNotFoundException error from lower layer
   * @exception InvalidFrameNumberException error from lower layer
   * @exception PageUnpinnedException error from lower layer
   * @exception ReplacerException error from lower layer
   * @throws PinPageException
   */
  public static void printCBitMap(String dbname)
      throws IOException, ConstructPageException, IteratorException, HashEntryNotFoundException,
      InvalidFrameNumberException, PageUnpinnedException, ReplacerException, PinPageException, InvalidTupleSizeException,
      UnpinPageException, HFException, HFBufMgrException, HFDiskMgrException {
    // Implementation of printBitMap starts here
    // for debug


    Heapfile hf = new Heapfile(dbname);
    System.out.println("");
    System.out.println("");
    System.out.println(dbname);
    System.out.println("---------------The Bit Map Structure---------------");


    Scan scan = hf.openScan();
    RID rid = new RID();
    byte[] rawTuple;
    Tuple tuple = null;
    while ((tuple = scan.getNext(rid)) != null) {
      try {
        //tuple = scan.getNext(rid);
        rawTuple = tuple.getTupleByteArray();
        int cnt = Convert.getIntValue(0, rawTuple);
        int bit = Convert.getShortValue(4, rawTuple);
        System.out.println("Count: " + cnt + ", " + "Bit: " + bit);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (tuple == null) {
        break;
      }
    }
    scan.closescan();
    //_printPage(hf.get_file_entry().pid);

    System.out.println("--------------- End ---------------");
    System.out.println("");
    System.out.println("");
  };
}
