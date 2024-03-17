package bitmap;

import diskmgr.*;
import global.*;

public class BM implements GlobalConst {
  public BM() {};

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
   */
  public static void printBitMap(bitmap.BitMapHeaderPage header)
    throws IOException,
      ConstructPageException,
      IteratorException,
      HashEntryNotFoundException,
      InvalidFrameNumberException,
      PageUnpinnedException,
      ReplacerException {
    // Implementation of printBitMap starts here
    // for debug
    if (header.get_rootId().pid == INVALID_PAGE) {
      System.out.println("The Bit Map is Empty!!!");
      return;
    }

    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("---------------The Bit Map Structure---------------");

    System.out.println(1 + "     " + header.get_rootId());

    _printPage(header.get_rootId());

    System.out.println("--------------- End ---------------");
    System.out.println("");
    System.out.println("");
  };

  private static void _printPage(PageId currentPageId) {
    BMPage bitMapPage = new BMPage(currentPageId);
    System.out.println("");
    System.out.println("**************To Print a Bit Map Page ********");
    System.out.println("Current Page ID: " + bitMapPage.getCurPage().pid);
    System.out.println("Previous Link: " + bitMapPage.getPrevPage().pid);
    System.out.println("Next Link: " + bitMapPage.getNextPage().pid);

    byte [] data = bitMapPage.getBMpageArray();
    int index = bitMapPage.DPFIXED; // 0 ~ DPFIXED - 1 is header, so we start from DPFIXED 
    for(index; index < data.length; index++) {
      for (int i = 7; i >= 0; i--) {
        // Use bitwise AND to check each bit
        int bit = (data[index] >> i) & 1;
        System.out.print(bit, " ");
      }
      System.out.println("");
    }
    System.out.println("************** END ********");
    System.out.println("");
    PageId nextPage = bitMapPage.getNextPage();
    if(nextPage.pid != INVALID_PAGE) {
      _printPage(nextPage);
    }
  }
}
