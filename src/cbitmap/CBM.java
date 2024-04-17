package cbitmap;

import java.io.IOException;
import btree.IteratorException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import global.*;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.Heapfile;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.Tuple;

public class CBM implements GlobalConst {
  public CBM() {};

  /**
   * For debug. Print the Compressed Bit map structure out
   *
   * @param CBMFile the target CBMFile
   * @exception IOException error from the lower layer
   * @exception ConstructPageException error from BM page constructor
   * @exception IteratorException error from iterator
   * @exception HashEntryNotFoundException error from lower layer
   * @exception InvalidFrameNumberException error from lower layer
   * @exception PageUnpinnedException error from lower layer
   * @exception ReplacerException error from lower layer
   * @throws PinPageException
   * @throws InvalidSlotNumberException
   * @throws bitmap.GetFileEntryException
   * @throws bitmap.UnpinPageException
   * @throws bitmap.UnpinPageException
   * @throws bitmap.GetFileEntryException
   * @throws bitmap.PinPageException
   */
  public static void printCBitMap(String dbname)
      throws IOException, ConstructPageException, IteratorException, HashEntryNotFoundException,
      InvalidFrameNumberException, PageUnpinnedException, ReplacerException, PinPageException,
      InvalidTupleSizeException, UnpinPageException, HFException, HFBufMgrException,
      HFDiskMgrException, GetFileEntryException, InvalidSlotNumberException,
      bitmap.UnpinPageException, bitmap.GetFileEntryException, bitmap.PinPageException {
    // Implementation of printCBitMap starts here
    // for debug


    System.out.println("");
    System.out.println("");
    System.out.println(dbname);
    System.out.println("---------------The Compressed Bit Map Structure---------------");


    CBitMapFile CBMFile = new CBitMapFile(dbname);
    Heapfile hf = CBMFile.compressedBMFile;
    Scan scan = hf.openScan();
    RID rid = new RID();
    byte[] rawTuple;
    // The first record is data info so we skip this
    // Tuple tuple = scan.getNext(rid);
    Tuple tuple = scan.getNext(rid);
    rawTuple = tuple.getTupleByteArray();
    int Cnt = Convert.getIntValue(0, rawTuple);
    int lastBit = Convert.getShortValue(4, rawTuple);
    int firstBit = Convert.getShortValue(6, rawTuple);
    System.out.println("Last Bit: " + lastBit + ", " + "Last Count: " + Cnt);
    System.out.println("First Bit: " + firstBit);
    while ((tuple = scan.getNext(rid)) != null) {
      try {
        // tuple = scan.getNext(rid);
        rawTuple = tuple.getTupleByteArray();
        int cnt = Convert.getIntValue(0, rawTuple);
        System.out.println("Count: " + cnt);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (tuple == null) {
        break;
      }
    }
    scan.closescan();
    // _printPage(hf.get_file_entry().pid);

    System.out.println("--------------- End ---------------");
    System.out.println("");
    System.out.println("");
  };
}
