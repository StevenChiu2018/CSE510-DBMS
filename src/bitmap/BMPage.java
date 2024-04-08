package bitmap;

import diskmgr.Page;
import global.*;
import heap.HFPage;
import java.io.IOException;

interface ConstSlot {
  int INVALID_SLOT = -1;
  int EMPTY_SLOT = -1;
}


public class BMPage extends HFPage implements ConstSlot {
  public static final int SIZE_OF_SLOT = 4;
  public static final int DPFIXED = 4 * 2 + 3 * 4;

  public static final int SLOT_CNT = 0;
  public static final int USED_PTR = 2;
  public static final int FREE_SPACE = 4;
  public static final int TYPE = 6;
  public static final int PREV_PAGE = 8;
  public static final int NEXT_PAGE = 12;
  public static final int CUR_PAGE = 16;

  /** number of slots in use */
  private short slotCnt;

  /** offset of first used byte by data records in data[] */
  private short usedPtr;

  /** number of bytes free in data[] */
  private short freeSpace;

  /** backward pointer to data page */
  private PageId prevPage = new PageId();

  /** forward pointer to data page */
  private PageId nextPage = new PageId();

  /** page number of this page */
  protected PageId curPage = new PageId();

  /** Default constructor */
  public BMPage() {}

  /**
   * Constructor of class BMPage open a BMPage and make this BMpage piont to the given page
   *
   * @param page the given page in Page type
   */
  public BMPage(Page page) {
    data = page.getpage();
  }

  /**
   * Returns the amount of available space on the page.
   *
   * @return the amount of available space on the page.
   * @exception IOException I/O errors
   */
  public int available_space() throws IOException {
    freeSpace = Convert.getShortValue(FREE_SPACE, data);
    return (freeSpace - SIZE_OF_SLOT);
  }

  /**
   * Dump contents of a page
   *
   * @exception IOException I/O error
   */
  public void dumpPage() throws IOException {
    int length, offset;

    curPage.pid = Convert.getIntValue(CUR_PAGE, data);
    nextPage.pid = Convert.getIntValue(NEXT_PAGE, data);
    usedPtr = Convert.getShortValue(USED_PTR, data);
    freeSpace = Convert.getShortValue(FREE_SPACE, data);
    slotCnt = Convert.getShortValue(SLOT_CNT, data);

    System.out.println("dumpPage");
    System.out.println("curPage= " + curPage.pid);
    System.out.println("nextPage= " + nextPage.pid);
    System.out.println("usedPtr= " + usedPtr);
    System.out.println("freeSpace= " + freeSpace);
    System.out.println("slotCnt= " + slotCnt);

    for (int i = 0, n = DPFIXED; i < slotCnt; n += SIZE_OF_SLOT, i++) {
      length = Convert.getShortValue(n, data);
      offset = Convert.getShortValue(n + 2, data);
      System.out.println("slotNo " + i + " offset= " + offset);
      System.out.println("slotNo " + i + " length= " + length);
    }
  }

  /**
   * Constructor of class BMPage open an existed BMpage.
   *
   * @param page the page to be opened
   */
  public void openBMpage(Page page) {
    data = page.getpage();
  }

  /**
   * Return current page.
   *
   * @return page number of current page
   * @exception IOException I/O errors
   */
  public PageId getCurPage() throws IOException {
    curPage.pid = Convert.getIntValue(CUR_PAGE, data);

    return curPage;
  }

  /**
   * @return page number of next page
   * @exception IOException I/O errors
   */
  public PageId getNextPage() throws IOException {
    nextPage.pid = Convert.getIntValue(NEXT_PAGE, data);
    return nextPage;
  }

  /**
   * @return PageId of previous page
   * @exception IOException I/O errors
   */
  public PageId getPrevPage() throws IOException {
    prevPage.pid = Convert.getIntValue(PREV_PAGE, data);
    return prevPage;
  }

  /**
   * @return byte array
   */
  public byte[] getBMpageArray() {
    return data;
  }

  /**
   * sets value of curPage to pageNo
   *
   * @param pageNo page number for current page
   * @exception IOException I/O errors
   */
  public void setCurPage(PageId pageNo) throws IOException {
    curPage.pid = pageNo.pid;
    Convert.setIntValue(curPage.pid, CUR_PAGE, data);
  }

  /**
   * sets value of nextPage to pageNo
   *
   * @param pageNo page number for next page
   * @exception IOException I/O errors
   */
  public void setNextPage(PageId pageNo) throws IOException {
    nextPage.pid = pageNo.pid;
    Convert.setIntValue(nextPage.pid, NEXT_PAGE, data);
  }

  /**
   * sets value of prevPage to pageNo
   *
   * @param pageNo page number for previous page
   * @exception IOException I/O errors
   */
  public void setPrevPage(PageId pageNo) throws IOException {
    prevPage.pid = pageNo.pid;
    Convert.setIntValue(prevPage.pid, PREV_PAGE, data);
  }

  /**
   * Constructor of class HFPage initialize a new page
   *
   * @param pageNo the page number of a new page to be initialized
   * @param apage the Page to be initialized
   * @see Page
   * @exception IOException I/O errors
   */
  public void init(PageId pageNo, Page apage) throws IOException {
    data = apage.getpage();

    slotCnt = 0; // no slots in use
    Convert.setShortValue(slotCnt, SLOT_CNT, data);

    curPage.pid = pageNo.pid;
    Convert.setIntValue(curPage.pid, CUR_PAGE, data);

    nextPage.pid = prevPage.pid = INVALID_PAGE;
    Convert.setIntValue(prevPage.pid, PREV_PAGE, data);
    Convert.setIntValue(nextPage.pid, NEXT_PAGE, data);

    usedPtr = (short) MAX_SPACE; // offset in data array (grow backwards)
    Convert.setShortValue(usedPtr, USED_PTR, data);

    freeSpace = (short) (MAX_SPACE - DPFIXED); // amount of space available
    Convert.setShortValue(freeSpace, FREE_SPACE, data);
  }

  public void setBit(int position, int bitOn) {
    position += BMPage.DPFIXED * 8;
    int byteIndex = position / 8;
    int bitOffset = position % 8;
    if (bitOn == 1) {
      // Set the bit to 1
      // Any bit | 1 equals 1
      data[byteIndex] = (byte) (data[byteIndex] | (1 << (7 - bitOffset)));
    } else {
      // Set the bit to 0
      // Any bit & 0 equals 0
      data[byteIndex] = (byte) (data[byteIndex] & ~(1 << (7 - bitOffset)));
    }
  }
}
