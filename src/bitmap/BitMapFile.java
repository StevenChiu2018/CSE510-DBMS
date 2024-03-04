package bitmap;

import diskmgr.Page;
import global.GlobalConst;

public class BitMapFile implements GlobalConst {
  private BitMapHeaderPage headerPage;
  private PageId headerPageId;
  private String dbname;

  /**
   * BitMapFile class an index file with given filename should already exist; this opens it.
   *
   * @param filename the Bit Map file name. Input parameter.
   * @exception GetFileEntryException can not ger the file from DB
   * @exception PinPageException failed when pin a page
   * @exception ConstructPageException BT page constructor failed
   */
  public BitMapFile(String filename)
      throws GetFileEntryException, PinPageException, ConstructPageException {
    // implementation start
    // headerPageId: the PageId of this BitMapFile's header page;
    this.headerPageId = get_file_entry(filename);
    this.headerPage = new BitMapHeaderPage(headerPageId);

    // valid and pinned - dbname contains a copy of the name of the database
    this.dbname = new String(filename);
  }

  /**
   * BitMapFile class: BitMapFile class; an index file with given filename should already exist,
   * then this opens it.
   *
   * @param filename the Bit Map file name. Input parameter
   * @exception GetFileEntryException can not ger the file from DB
   * @exception PinPageException failed when pin a page
   * @exception ConstructPageException BT page constructor failed
   */
  public BitMapFile(String filename)
      throws GetFileEntryException, PinPageException, ConstructPageException {
    // implementation start
    // headerPageId: the PageId of this BitMapFile's header page;
    this.headerPageId = get_file_entry(filename);
    this.headerPage = new BitMapHeaderPage(headerPageId);

    // valid and pinned - dbname contains a copy of the name of the database
    this.dbname = new String(filename);
  }

  /**
   * BitMapFile class: BitMapFile class; an index file with given filename should not already exist;
   * this creates the BitMap file from scratch.
   *
   * @param filename the Bit Map file name. Input parameter
   * @param columnFile
   * @param columnNo
   * @param value
   * @exception GetFileEntryException can not get file
   * @exception ConstructPageException page constructor failed
   * @exception IOException error from lower layer
   * @exception AddFileEntryException can not add file into DB
   */
  public BitMapFile(String filename, Columnarfile columnFile, int ColumnNo, valueClass value)
      throws GetFileEntryException, ConstructPageException, IOException, AddFileEntryException {
    // implementation start
    this.headerPageId = get_file_entry(filename);
    if (this.headerPageId == null) { // file not exist
      this.headerPage = new BitMapHeaderPage();
      this.headerPageId = this.headerPage.getPageId();
      add_file_entry(filename, this.headerPageId);
      this.headerPage.set_magic0(MAGIC0);
      this.headerPage.set_rootId(new PageId(INVALID_PAGE));
      this.headerPage.setType(NodeType.BTHEAD);
    }
    dbname = new String(filename);
  }

  /**
   * Access method to data member.
   *
   * @return Return a BitMapHeaderPage object that is the header page of this bit map file.
   */
  public BitMapHeaderPage getHeaderPage() {
    return this.headerPage;
  }

  /**
   * Close the Bit Map file. Unpin header page.
   *
   * @exception PageUnpinnedException error from the lower layer
   * @exception InvalidFrameNumberException error from the lower layer
   * @exception HashEntryNotFoundException error from the lower layer
   * @exception ReplacerException error from the lower layer
   */
  public void close()
      throws PageUnpinnedException,
          InvalidFrameNumberException,
          HashEntryNotFoundException,
          ReplacerException {
    // Implementation start
    if (headerPage != null) {
      SystemDefs.JavabaseBM.unpinPage(this.headerPageId, true);
      this.headerPage = null;
    }
  }

  /**
   * Destroy entire bit map file.
   *
   * @exception IOException error from the lower layer
   * @exception IteratorException iterator error
   * @exception UnpinPageException error when unpin a page
   * @exception FreePageException error when free a page
   * @exception DeleteFileEntryException failed when delete a file from DM
   * @exception ConstructPageException error in BM page constructor
   * @exception PinPageException failed when pin a page
   */
  public void destroyBitMapFile()
      throws IOException,
          IteratorException,
          UnpinPageException,
          FreePageException,
          DeleteFileEntryException,
          ConstructPageException,
          PinPageException {
    // Implementation start
    if (headerPage != null) {
      this.unpinPage(this.headerPageId);
      this.freePage(this.headerPageId);
      this.delete_file_entry(this.dbname);
      this.headerPage = null;
      this.headerPageId = null;
      this.dbname = null;
    }
  }

  public boolean Delete(int position) throws UnpinPageException, PinPageException, IOException {
    // Implementation start
    PageId pageno = this.headerPage.get_rootId();
    if (pageno.pid != INVALID_PAGE) {
      Page page = pinPage(pageno);
      page.setBit(position, 0);
      unpinPage(pageno);
    }
  }

  public boolean Insert(int position) throws UnpinPageException, PinPageException, IOException {
    // Implementation start
    PageId pageno = this.headerPage.get_rootId();
    if (pageno.pid != INVALID_PAGE) {
      Page page = pinPage(pageno);
      page.setBit(position, 1);
      unpinPage(pageno);
    }
  }

  private Page pinPage(PageId pageno) throws PinPageException {
    try {
      Page page = new Page();
      SystemDefs.JavabaseBM.pinPage(pageno, page, false /* Rdisk */);
      return page;
    } catch (Exception e) {
      e.printStackTrace();
      throw new PinPageException(e, "BitMapFile.java: pinPage() failed");
    }
  }

  private void unpinPage(PageId pageno) throws UnpinPageException {
    try {
      SystemDefs.JavabaseBM.unpinPage(pageno, false /* = not DIRTY */);
    } catch (Exception e) {
      e.printStackTrace();
      throw new UnpinPageException(e, "BitMapFile.java: unpinPage() failed");
    }
  }

  private void unpinPage(PageId pageno, boolean dirty) throws UnpinPageException {
    try {
      SystemDefs.JavabaseBM.unpinPage(pageno, dirty);
    } catch (Exception e) {
      e.printStackTrace();
      throw new UnpinPageException(e, "BitMapFile.java: unpinPage() failed");
    }
  }

  private void freePage(PageId pageno) throws FreePageException {
    try {
      SystemDefs.JavabaseBM.freePage(pageno);
    } catch (Exception e) {
      e.printStackTrace();
      throw new FreePageException(e, "BitMapFile.java: freepage() failed");
    }
  }

  private void add_file_entry(String filename, PageId pageno) throws HFDiskMgrException {
    try {
      SystemDefs.JavabaseDB.add_file_entry(filename, pageno);
    } catch (Exception e) {
      throw new AddFileEntryException(e, "BitMapFile.java: add_file_entry() failed");
    }
  } // end of add_file_entry

  private PageId get_file_entry(String filename) throws HFDiskMgrException {
    PageId tmpId = new PageId();
    try {
      tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
    } catch (Exception e) {
      throw new GetFileEntryException(e, "BitMapFile.java: get_file_entry() failed");
    }
    return tmpId;
  } // end of get_file_entry

  private void delete_file_entry(String filename) throws HFDiskMgrException {
    try {
      SystemDefs.JavabaseDB.delete_file_entry(filename);
    } catch (Exception e) {
      e.printStackTrace();
      throw new DeleteFileEntryException(e, "BitMapFile.java: delete_file_entry() failed");
    }
  } // end of delete_file_entry
}
