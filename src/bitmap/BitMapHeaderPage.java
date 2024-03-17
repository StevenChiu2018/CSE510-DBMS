package bitmap;

import bufmgr.*;
import diskmgr.Page;
import global.*;
import heap.*;
import java.io.*;

/**
 * Intefrace of a bit map index header page. Here we use a HFPage as head page of the file Inside
 * the headpage Logicaly, there are only seven elements inside the head page, they are magic0,
 * rootId, keyType, maxKeySize, deleteFashion, and type(=NodeType.BTHEAD)
 */
public class BitMapHeaderPage extends HFPage {

  /** pin the page with pageno, and get the corresponding Page */
  public BitMapHeaderPage(PageId pageno) throws ConstructPageException {
    super();
    try {
      SystemDefs.JavabaseBM.pinPage(pageno, this, false /* Rdisk */);
    } catch (Exception e) {
      throw new ConstructPageException(e, "pinpage failed");
    }
  }

  /** associate the Page instance with the Page instance */
  public BitMapHeaderPage(Page page) {
    super(page);
  }

  /** new a page, and associate the Page instance with the Page instance */
  public BitMapHeaderPage() throws ConstructPageException {
    super();
    try {
      Page apage = new Page();
      PageId pageId = SystemDefs.JavabaseBM.newPage(apage, 1);
      if (pageId == null)
        throw new ConstructPageException(null, "new page failed");
      this.init(pageId, apage);
    } catch (Exception e) {
      throw new ConstructPageException(e, "construct header page failed");
    }
  }

  void setPageId(PageId pageno) throws IOException {
    setCurPage(pageno);
  }

  PageId getPageId() throws IOException {
    return getCurPage();
  }

  /**
   * set the magic0. setPrevPage: heap/HFPage.java
   *
   * @param magic magic0 will be set to be equal to magic.
   */
  void set_magic0(int magic) throws IOException {
    setPrevPage(new PageId(magic));
  }

  /** get the magic0. getPrevPage: heap/HFPage.java */
  int get_magic0() throws IOException {
    return getPrevPage().pid;
  }

  /** set the rootId. setNextPage: heap/HFPage.java */
  void set_rootId(PageId rootID) throws IOException {
    setNextPage(rootID);
  }

  /** get the rootId. getNextPage: heap/HFPage.java */
  public PageId get_rootId() throws IOException {
    return getNextPage();
  }

  /** set the colNo */
  void set_ColNo(int colNo) throws IOException {
    setSlot(1, colNo, 0);
  }

  /** set the value */
  void set_value(ValueClass value) throws IOException {
    setSlot(2, value, 0);
  }
} // end of BTreeHeaderPage
