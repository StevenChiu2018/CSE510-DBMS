package cbitmap;

import java.io.IOException;
import bitmap.BitMapFile;
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
import global.AttrType;
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

public class CBitMapFile extends BitMapFile {
  public HFPage firstPage;
  private PageId firstPageId;
  public int lastCnt = -1;
  public short lastBit = -1;
  public short firstBit = -1;
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
   * @throws InvalidSlotNumberException
   * @throws UnpinPageException
   * @throws bitmap.GetFileEntryException
   * @throws bitmap.UnpinPageException
   * @throws bitmap.UnpinPageException
   * @throws bitmap.GetFileEntryException
   * @throws bitmap.PinPageException
   */
  public CBitMapFile(String filename)
      throws GetFileEntryException, PinPageException, ConstructPageException, HFDiskMgrException,
      IOException, HFException, HFBufMgrException, UnpinPageException, InvalidSlotNumberException,
      bitmap.UnpinPageException, bitmap.GetFileEntryException, bitmap.PinPageException {
    // implementation start
    // firstId: the PageId of this BitMapFile's first page;
    this.compressedBMFile = new Heapfile(filename);
    this.firstPageId = get_file_entry(filename);
    this.dbname = new String(filename);
    this.firstPage = new HFPage(this.pinPage(this.firstPageId));
    this.infoRecordRID = this.firstPage.firstRecord();
    Tuple infoRecordTuple = this.firstPage.getRecord(this.infoRecordRID);
    byte[] infoRecord = infoRecordTuple.getTupleByteArray();
    this.lastCnt = Convert.getIntValue(0, infoRecord);
    this.lastBit = Convert.getShortValue(4, infoRecord);
    this.firstBit = Convert.getShortValue(6, infoRecord);
    this.unpinPage(this.firstPageId);
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
      PageUnpinnedException, ReplacerException, HFException, InvalidSlotNumberException,
      SpaceNotAvailableException, Exception {
    // implementation start
    this.firstPageId = get_file_entry(filename);

    // If there is no data in the first page, initialize it.
    if (this.firstPageId == null) {
      // initialize lastBit and lastCnt
      byte[] infoRecord = new byte[8];
      Convert.setIntValue(this.lastCnt, 0, infoRecord);
      Convert.setShortValue(this.lastBit, 4, infoRecord);
      Convert.setShortValue(this.firstBit, 6, infoRecord);
      this.compressedBMFile = new Heapfile(filename);
      this.infoRecordRID = this.compressedBMFile.insertRecord(infoRecord);
    } else {
      this.compressedBMFile = new Heapfile(filename);
      this.firstPage = new HFPage(this.pinPage(this.firstPageId));
      this.infoRecordRID = this.firstPage.firstRecord();
      Tuple infoRecordTuple = this.firstPage.getRecord(this.infoRecordRID);
      byte[] infoRecord = infoRecordTuple.getTupleByteArray();
      this.lastCnt = Convert.getIntValue(0, infoRecord);
      this.lastBit = Convert.getShortValue(4, infoRecord);
      this.firstBit = Convert.getShortValue(6, infoRecord);
      this.unpinPage(this.firstPageId);
    }

    this.dbname = new String(filename);
    if (this.lastBit == -1 && this.lastCnt == -1) {
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
  private void storeCompressedBMTuple()
      throws IOException, InvalidSlotNumberException, InvalidTupleSizeException,
      SpaceNotAvailableException, HFException, HFBufMgrException, HFDiskMgrException {
    byte[] tupleData = new byte[4];
    int offset = 0;
    Convert.setIntValue(this.lastCnt, offset, tupleData);
    // Convert.setShortValue(this.lastBit, offset + 4, tupleData);
    this.compressedBMFile.insertRecord(tupleData);
  }

  public void createCBitMap(Columnarfile columnFile, int ColumNo, ByteValue value)
      throws UnpinPageException, PinPageException, IOException, InvalidTupleSizeException,
      HFBufMgrException, ConstructPageException, IteratorException, HashEntryNotFoundException,
      InvalidFrameNumberException, PageUnpinnedException, ReplacerException,
      InvalidSlotNumberException, SpaceNotAvailableException, HFException, HFDiskMgrException,
      InvalidUpdateException, Exception {
    RID rid = new RID();
    Scan columnScan = columnFile.openColumnScan(ColumNo);
    Tuple tuple;
    CBM cbm = new CBM();

    // Scan through the columnFile to find the values to be indexed
    while ((tuple = columnScan.getNext(rid)) != null) {
      boolean isEqual = false;
      if (value.type == AttrType.attrInteger) {
        int targetValue = Convert.getIntValue(0, value.value);
        int curValue = Convert.getIntValue(0, tuple.getTupleByteArray());
        isEqual = (curValue == targetValue);
      } else {
        String targetValue = Convert.getStrValue(0, value.value, value.size);
        String curValue = Convert.getStrValue(0, tuple.getTupleByteArray(), value.size);
        isEqual = (curValue.equals(targetValue));
      }
      // record the first bit in the infoRecord
      short targetBit = (short) (isEqual ? 1 : 0);
      if (this.firstBit == -1) {
        this.firstBit = targetBit;
      } else if (targetBit == this.lastBit) {
        this.lastCnt++;
      } else {
        if (this.lastBit != -1) {
          storeCompressedBMTuple();
        }
        this.lastBit = targetBit;
        this.lastCnt = 1;
      }
    }
    columnScan.closescan();


    storeCompressedBMTuple();

    byte[] infoRecord = new byte[8];
    int offset = 0;
    // initialize lastBit and lastCnt
    Convert.setIntValue(this.lastCnt, offset, infoRecord);
    Convert.setShortValue(this.lastBit, offset + 4, infoRecord);
    Convert.setShortValue(this.firstBit, offset + 6, infoRecord);
    Tuple infoTuple = new Tuple(infoRecord, offset, 8);
    this.compressedBMFile.updateRecord(this.infoRecordRID, infoTuple);
  }

  /**
   * Access method to data member.
   *
   * @return Return a CBitMapfirstPage object that is the first page of this bit map file.
   */
  public HFPage getFirstPage() {
    return this.firstPage;
  }

  /**
   * Close the Compressed Bit Map file. Unpin header page.
   */
  public void close() {
    // Implementation start
    if (firstPage != null) {
      // this.unpinPage(this.firstPageId, true);
      this.firstPage = null;
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
  public void destroyCBitMapFile() throws IOException, IteratorException, UnpinPageException,
      FreePageException, DeleteFileEntryException, ConstructPageException, PinPageException,
      HFDiskMgrException, InvalidSlotNumberException, FileAlreadyDeletedException,
      InvalidTupleSizeException, HFBufMgrException {
    // Implementation start
    compressedBMFile.deleteFile();
  }
}
