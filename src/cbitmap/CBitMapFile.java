package cbitmap;

import java.io.IOException;
import btree.IteratorException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import columnar.Columnarfile;
import diskmgr.DiskMgrException;
import diskmgr.FileIOException;
import diskmgr.InvalidPageNumberException;
import diskmgr.Page;
import global.ByteValue;
import global.Convert;
import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;
import heap.FileAlreadyDeletedException;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidTupleSizeException;
import heap.InvalidUpdateException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import heap.HFPage;
import heap.Heapfile;
import heap.InvalidSlotNumberException;

public class CBitMapFile implements GlobalConst {
  public HFPage headerPage;
  private PageId headerPageId;
  private String dbname;
  public int lastCnt = 0;
  public short lastBit = 0;
  public short firstBit = 0;
  public Heapfile compressedBMFile;
  public RID infoRecordRID = new RID();

  /**
   * BitMapFile class a bit map file with given filename should already exist; this opens it.
   *
   * @param filename the Bit Map file name. Input parameter.
   * @exception GetFileEntryException can not ger the file from DB
   * @exception PinPageException failed when pin a page
   * @exception ConstructPageException BT page constructor failed
   * @throws HFDiskMgrException
   * @throws IOException
   * @throws HFBufMgrException 
   * @throws HFException 
   */
  public CBitMapFile(String filename) throws GetFileEntryException, PinPageException,
      ConstructPageException, HFDiskMgrException, IOException, HFException, HFBufMgrException {
    // implementation start
    // headerPageId: the PageId of this BitMapFile's header page;
    this.compressedBMFile = new Heapfile(filename);
    this.headerPageId = get_file_entry(filename);
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
   * @throws HFBufMgrException
   * @throws ReplacerException
   * @throws PageUnpinnedException
   * @throws InvalidFrameNumberException
   * @throws HashEntryNotFoundException
   * @throws IteratorException
   * @throws HFException 
   * @throws InvalidSlotNumberException 
   * @throws SpaceNotAvailableException 
   */
  public CBitMapFile(String filename, Columnarfile columnFile, int columnNo, ByteValue value)
      throws GetFileEntryException, ConstructPageException, IOException, AddFileEntryException,
      HFDiskMgrException, UnpinPageException, PinPageException, InvalidTupleSizeException,
      HFBufMgrException, IteratorException, HashEntryNotFoundException, InvalidFrameNumberException,
      PageUnpinnedException, ReplacerException, HFException, InvalidSlotNumberException, SpaceNotAvailableException,
      Exception {
    // implementation start
    this.headerPageId = get_file_entry(filename);

    // get the lastBit and lastCnt from header page
    byte [] infoRecord = new byte[8];
    int offset = 0;
    // If there is no data in the first page, initialize it.
    if (this.headerPageId == null) {
      int bitCount = -1;
      Short bitType = -1;
      Short firstBit = -1;
      // initialize lastBit and lastCnt
      Convert.setIntValue(bitCount, offset, infoRecord);
      Convert.setShortValue(bitType, offset + 4, infoRecord);
      Convert.setShortValue(firstBit, offset + 6, infoRecord);
      this.compressedBMFile = new Heapfile(filename);
      this.infoRecordRID = this.compressedBMFile.insertRecord(infoRecord);
    } else {
      this.compressedBMFile = new Heapfile(filename);
    }
    this.dbname = new String(filename);
    // get lastBit and lastCnt from infoRecord
    Scan scan = this.compressedBMFile.openScan();
    Tuple firstTuple = scan.getNext(infoRecordRID);
    infoRecord = firstTuple.getTupleByteArray();
    scan.closescan();
    this.lastCnt = Convert.getIntValue(offset, infoRecord);
    this.lastBit = Convert.getShortValue(offset + 4, infoRecord);
    this.firstBit = Convert.getShortValue(offset + 6, infoRecord);
    if(this.lastBit == -1 && this.lastCnt == -1) {
      this.createCBitMap(columnFile, columnNo, value);
    }
  }

  /**
   * @throws IOException
   * @throws InvalidSlotNumberException
   * @throws InvalidTupleSizeException
   * @throws SpaceNotAvailableException
   * @throws HFException
   * @throws HFBufMgrException
   * @throws HFDiskMgrException
   */
  private void storeCompressedBMTuple() throws IOException, InvalidSlotNumberException, InvalidTupleSizeException, SpaceNotAvailableException, HFException, HFBufMgrException, HFDiskMgrException {
    byte [] tupleData = new byte[4];
    int offset = 0;
    Convert.setIntValue(this.lastCnt, offset, tupleData);
    //Convert.setShortValue(this.lastBit, offset + 4, tupleData);
    this.compressedBMFile.insertRecord(tupleData);
  }

  public void createCBitMap(Columnarfile columnFile, int ColumNo, ByteValue value)
      throws UnpinPageException, PinPageException, IOException, InvalidTupleSizeException,
      HFBufMgrException, ConstructPageException, IteratorException, HashEntryNotFoundException,
      InvalidFrameNumberException, PageUnpinnedException, ReplacerException, InvalidSlotNumberException,
      SpaceNotAvailableException, HFException, HFDiskMgrException, InvalidUpdateException, Exception {
    RID rid = new RID();
    Scan columnScan = columnFile.openColumnScan(ColumNo);
    Tuple tuple;
    CBM cbm = new CBM();

    // Scan through the columnFile to find the values to be indexed
    while ((tuple = columnScan.getNext(rid)) != null) {
      boolean isEqual = false;
      if (value.type == 1) {
        int targetValue = Convert.getIntValue(0, value.value);
        int curValue = Convert.getIntValue(0, tuple.getTupleByteArray());
        isEqual = (curValue == targetValue);
      } else {
        String targetValue = Convert.getStrValue(0, value.value, value.size);
        String curValue = Convert.getStrValue(0, tuple.getTupleByteArray(), value.size);
        isEqual = (curValue.equals(targetValue));
      }
      // record the first bit in the infoRecord
      if(this.firstBit == -1) {
        if(isEqual) {
          this.firstBit = 1;
        } else {
          this.firstBit = 0;
        }
      }
      if(isEqual) {
        // bit == 1
        if(this.lastBit == 1) {
          this.lastCnt ++;
        } else if (this.lastBit == 0){
          // store previous and count new one
          storeCompressedBMTuple();
          this.lastBit = 1;
          this.lastCnt = 1;
        } else {
          // -1
          // Do not store previous
          this.lastBit = 1;
          this.lastCnt = 1;
        }

      } else {
        // bit == 0 
        if(lastBit == 1) {
          storeCompressedBMTuple();
          // store previous and count new one
          this.lastBit = 0;
          this.lastCnt = 1;
        } else if (this.lastBit == 0){
          this.lastCnt ++;
        } else {
          // -1 means it's the first one
          // Do not store previous
          this.lastBit = 0;
          this.lastCnt = 1;
        }
      }
    }
    columnScan.closescan();


    storeCompressedBMTuple();

    byte [] infoRecord = new byte[8];
    int offset = 0;
    // initialize lastBit and lastCnt
    Convert.setIntValue(this.lastCnt, offset, infoRecord);
    Convert.setShortValue(this.lastBit, offset + 4, infoRecord);
    Convert.setShortValue(this.firstBit, offset + 6, infoRecord);
    Tuple infoTuple = new Tuple(infoRecord, offset, 8);
    this.compressedBMFile.updateRecord(this.infoRecordRID, infoTuple);

    // Use this to print out index file
    // cbm.printCBitMap(this.dbname);
  }

  /**
   * Access method to data member.
   *
   * @return Return a BitMapHeaderPage object that is the header page of this bit map file.
   */
  public HFPage getHeaderPage() {
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
      //this.unpinPage(this.headerPageId, true);
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
   * @throws HFBufMgrException 
   * @throws InvalidTupleSizeException 
   * @throws FileAlreadyDeletedException 
   * @throws InvalidSlotNumberException 
   */
  public void destroyCBitMapFile()
      throws IOException, IteratorException, UnpinPageException, FreePageException,
      DeleteFileEntryException, ConstructPageException, PinPageException, HFDiskMgrException, InvalidSlotNumberException, FileAlreadyDeletedException, InvalidTupleSizeException, HFBufMgrException {
    // Implementation start
    compressedBMFile.deleteFile();
  }

  private Page pinPage(PageId pageno) throws PinPageException {
    try {
      Page page = new Page();
      SystemDefs.JavabaseBM.pinPage(pageno, page, true /* Rdisk */);
      return page;
    } catch (Exception e) {
      e.printStackTrace();
      throw new PinPageException(e, "CBitMapFile.java: pinPage() failed");
    }
  }

  private void unpinPage(PageId pageno) throws UnpinPageException {
    try {
      SystemDefs.JavabaseBM.unpinPage(pageno, true /* = DIRTY */);
    } catch (Exception e) {
      e.printStackTrace();
      throw new UnpinPageException(e, "CBitMapFile.java: unpinPage() failed");
    }
  }

  private void unpinPage(PageId pageno, boolean dirty) throws UnpinPageException {
    try {
      SystemDefs.JavabaseBM.unpinPage(pageno, dirty);
    } catch (Exception e) {
      e.printStackTrace();
      throw new UnpinPageException(e, "CBitMapFile.java: unpinPage() failed");
    }
  }

  public PageId get_file_entry(String filename) throws HFDiskMgrException, GetFileEntryException {
    PageId tmpId;
    try {
      tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
    } catch (IOException e) {
      System.out.println(e.getMessage());
      throw new GetFileEntryException(e, "CBitMapFile.java: get_file_entry() failed");
    } catch (FileIOException e) {
      System.out.println(e.getMessage());
      throw new GetFileEntryException(e, "CBitMapFile.java: get_file_entry() failed");
    } catch (InvalidPageNumberException e) {
      System.out.println(e.getMessage());
      throw new GetFileEntryException(e, "CBitMapFile.java: get_file_entry() failed");
    } catch (DiskMgrException e) {
      System.out.println(e.getMessage());
      throw new GetFileEntryException(e, "CBitMapFile.java: get_file_entry() failed");
    }
    return tmpId;
  } // end of get_file_entry
}
