package bitmap;

import java.io.IOException;
import btree.IndexFile;
import btree.IteratorException;
import btree.NodeType;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import columnar.Columnarfile;
import diskmgr.Page;
import global.ByteValue;
import global.Convert;
import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;
import global.ValueClass;
import heap.HFDiskMgrException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.Tuple;

public class BitMapFile implements GlobalConst {
  private static final int MAGIC0 = 1989;
  public BitMapHeaderPage headerPage;
  private PageId headerPageId;
  private String dbname;

  /**
   * BitMapFile class a bit map file with given filename should already exist; this opens it.
   *
   * @param filename the Bit Map file name. Input parameter.
   * @exception GetFileEntryException can not ger the file from DB
   * @exception PinPageException failed when pin a page
   * @exception ConstructPageException BT page constructor failed
   * @throws HFDiskMgrException
   * @throws IOException
   */
  public BitMapFile(String filename) throws GetFileEntryException, PinPageException,
      ConstructPageException, HFDiskMgrException, IOException {
    // implementation start
    // headerPageId: the PageId of this BitMapFile's header page;
    this.headerPageId = this.get_file_entry(filename);
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
   * @throws HFDiskMgrException
   * @throws InvalidTupleSizeException
   * @throws PinPageException
   * @throws UnpinPageException
   */
  public BitMapFile(String filename, Columnarfile columnFile, int columnNo, ByteValue value)
      throws GetFileEntryException, ConstructPageException, IOException, AddFileEntryException,
      HFDiskMgrException, UnpinPageException, PinPageException, InvalidTupleSizeException {
    // implementation start
    this.headerPageId = get_file_entry(filename);
    if (this.headerPageId == null) { // file not exist
      this.headerPage = new BitMapHeaderPage();
      this.headerPageId = this.headerPage.getPageId();
      this.add_file_entry(filename, this.headerPageId);
      this.headerPage.set_magic0(MAGIC0);
      this.headerPage.set_rootId(new PageId(INVALID_PAGE));
      this.headerPage.setType(NodeType.BTHEAD);
      this.headerPage.set_ColNo(columnNo);
    } else {
      this.headerPage = new BitMapHeaderPage(this.headerPageId);
    }
    dbname = new String(filename);

    this.createBitMap(columnFile, columnNo, value);
  }

  public void createBitMap(Columnarfile columnFile, int ColumNo, ByteValue value)
      throws UnpinPageException, PinPageException, IOException, InvalidTupleSizeException {
    int position = 0;
    RID rid = new RID();
    Scan columnScan = columnFile.openColumnScan(ColumNo);
    Tuple tuple;

    while ((tuple = columnScan.getNext(rid)) != null) {
      boolean isEqual = false;

      if (value.type == 1) {
        int targetValue = Convert.getIntValue(0, value.value);
        int curValue = Convert.getIntValue(0, tuple.getTupleByteArray());
        isEqual = (curValue == targetValue);
      } else {
        String targetValue = Convert.getStrValue(0, value.value, value.size);
        String curValue = Convert.getStrValue(0, tuple.getTupleByteArray(), value.size);
        isEqual = (curValue == targetValue);
      }
      if (isEqual) {
        insert(position);
      } else {
        delete(position);
      }

      position++;
    }
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
   * @throws UnpinPageException
   */
  public void close() throws PageUnpinnedException, InvalidFrameNumberException,
      HashEntryNotFoundException, ReplacerException, UnpinPageException {
    // Implementation start
    if (headerPage != null) {
      this.unpinPage(this.headerPageId, true);
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
   * @throws HFDiskMgrException
   */
  public void destroyBitMapFile()
      throws IOException, IteratorException, UnpinPageException, FreePageException,
      DeleteFileEntryException, ConstructPageException, PinPageException, HFDiskMgrException {
    // Implementation start
    if (this.headerPage != null) {
      // Destroy Data Page
      PageId pageno = this.headerPage.get_rootId();
      if (pageno.pid != INVALID_PAGE) {
        _destroyFile(pageno);
      }
      // Destroy Header Page
      this.unpinPage(this.headerPageId);
      this.freePage(this.headerPageId);
      this.delete_file_entry(this.dbname);
      this.headerPage = null;
      this.headerPageId = null;
      this.dbname = null;
    }
  }

  // resursively free all the data pages in the file
  private void _destroyFile(PageId pageno) throws IOException, IteratorException, PinPageException,
      ConstructPageException, UnpinPageException, FreePageException {
    // Implementation start
    Page curPage = pinPage(pageno);
    BMPage currentPage = new BMPage(curPage);
    PageId nextPage = currentPage.getNextPage();
    if (nextPage.pid != INVALID_PAGE) {
      _destroyFile(nextPage);
    }
    this.unpinPage(pageno);
    this.freePage(pageno);
  }

  public boolean delete(int position) throws UnpinPageException, PinPageException, IOException {
    // Implementation start
    PageId pageno = this.headerPage.get_rootId();
    // return false if there is no header page
    if (pageno.pid == INVALID_PAGE) {
      return false;
    }

    PageId targetPageNo = this.headerPage.get_rootId();
    Page targetPage = pinPage(targetPageNo);
    BMPage targetBMPage = new BMPage(targetPage);

    while (position >= MINIBASE_PAGESIZE * 4) {
      // return false if there is no target page
      if (targetPageNo.pid == INVALID_PAGE) {
        return false;
      }

      position = position - MINIBASE_PAGESIZE * 4;
      PageId nextTargetPageNo = targetBMPage.getNextPage();
      unpinPage(targetPageNo);
      targetPageNo = nextTargetPageNo;
      targetPage = pinPage(targetPageNo);
      targetBMPage = new BMPage(targetPage);
    }

    targetBMPage.setBit(position, 0);
    this.unpinPage(targetPageNo);

    return true;
  }

  public boolean insert(int position) throws UnpinPageException, PinPageException, IOException {
    // Implementation start
    PageId pageno = this.headerPage.get_rootId();
    // If there is no headerpage, create one
    if (pageno.pid == INVALID_PAGE) {
      BMPage newPage = new BMPage();
      PageId newPageNo = newPage.getCurPage();
      pinPage(newPageNo);
      newPage.setNextPage(new PageId(INVALID_PAGE));
      this.headerPage.set_rootId(newPageNo);
      unpinPage(newPageNo);
    }
    // Find the target page we want to insert a bit
    PageId targetPageNo = this.headerPage.get_rootId();
    Page targetPage = pinPage(targetPageNo);
    BMPage targetBMPage = new BMPage(targetPage);

    PageId parentPageNo = targetPageNo;
    while (position >= MINIBASE_PAGESIZE * 4) {
      // if there is not existed page, create one.
      if (targetPageNo.pid == INVALID_PAGE) {
        BMPage newPage = new BMPage();
        targetPageNo = newPage.getCurPage();
        targetPage = pinPage(targetPageNo);
        targetBMPage = new BMPage(targetPage);

        // set the next of current page as the new page we created
        Page parentPage = pinPage(parentPageNo);
        BMPage parentBMPage = new BMPage(parentPage);
        parentBMPage.setNextPage(targetPageNo);
        unpinPage(parentPageNo);

        // set the next of the new page page we created as -1
        targetBMPage.setNextPage(new PageId(INVALID_PAGE));
      }

      position = position - MINIBASE_PAGESIZE * 4;
      // find the next page
      PageId nextTargetPageNo = targetBMPage.getNextPage();
      parentPageNo = targetPageNo;
      unpinPage(targetPageNo);
      targetPageNo = nextTargetPageNo;
      targetPage = pinPage(targetPageNo);
      targetBMPage = new BMPage(targetPage);
    }
    // Do insert
    targetBMPage.setBit(position, 1);
    this.unpinPage(targetPageNo);
    return true;
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

  private void add_file_entry(String filename, PageId pageno)
      throws HFDiskMgrException, AddFileEntryException {
    try {
      SystemDefs.JavabaseDB.add_file_entry(filename, pageno);
    } catch (Exception e) {
      throw new AddFileEntryException(e, "BitMapFile.java: add_file_entry() failed");
    }
  } // end of add_file_entry

  public PageId get_file_entry(String filename) throws HFDiskMgrException, GetFileEntryException {
    PageId tmpId = new PageId();
    try {
      tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
    } catch (Exception e) {
      throw new GetFileEntryException(e, "BitMapFile.java: get_file_entry() failed");
    }
    return tmpId;
  } // end of get_file_entry

  private void delete_file_entry(String filename)
      throws HFDiskMgrException, DeleteFileEntryException {
    try {
      SystemDefs.JavabaseDB.delete_file_entry(filename);
    } catch (Exception e) {
      e.printStackTrace();
      throw new DeleteFileEntryException(e, "BitMapFile.java: delete_file_entry() failed");
    }
  } // end of delete_file_entry
}
