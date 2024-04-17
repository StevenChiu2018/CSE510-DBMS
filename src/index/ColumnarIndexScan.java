package index;

import btree.*;
import bufmgr.*;
import cbitmap.CBitMapFile;
import cbitmap.UnpinPageException;
import columnar.Columnarfile;
import diskmgr.*;
import global.*;
import heap.*;
import iterator.*;
import java.io.*;
import bitmap.BitMapFile;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Index Scan iterator will directly access the required tuple using the provided key. It will also
 * perform selections and projections. information about the tuples and the index are passed to the
 * constructor, then the user calls <code>get_next()</code> to get the tuples.
 */
public class ColumnarIndexScan extends Iterator {
  public FldSpec[] perm_mat;
  private BitMapFile[] BMFiles;
  private Columnarfile columnarFile;
  private Tuple tuple1;
  private int scanIndex;
  private Scan tidHeapScanner;
  private ArrayList<Integer> distinctColPos;
  private ArrayList<TID> scannedTID;


  /**
   * class constructor. set up the index scan.
   *
   * @param relName name of the input relation
   * @param index array of type of the index (B_Index, Hash)
   * @param indName array of name of the input index
   * @exception IndexException error from the lower layer
   * @exception InvalidTypeException tuple type not valid
   * @exception InvalidTupleSizeException tuple size not valid
   * @exception UnknownIndexTypeException index type unknown
   * @exception IOException from the lower layer
   * @throws HFDiskMgrException
   * @throws ConstructPageException
   * @throws PinPageException
   * @throws GetFileEntryException
   * @throws cbitmap.GetFileEntryException
   * @throws cbitmap.PinPageException
   * @throws cbitmap.ConstructPageException
   * @throws bitmap.UnpinPageException
   * @throws UnpinPageException
   * @throws InvalidSlotNumberException
   * @throws HFBufMgrException
   * @throws HFException
   * @throws ReplacerException
   * @throws HashEntryNotFoundException
   * @throws InvalidFrameNumberException
   * @throws PageUnpinnedException
   */
  public ColumnarIndexScan(final String relName, IndexType[] index, final String[] indName)
      throws IndexException, InvalidTypeException, InvalidTupleSizeException,
      UnknownIndexTypeException, IOException, GetFileEntryException, PinPageException,
      ConstructPageException, HFDiskMgrException, cbitmap.GetFileEntryException,
      cbitmap.PinPageException, cbitmap.ConstructPageException, HFException, HFBufMgrException,
      InvalidSlotNumberException, UnpinPageException, bitmap.UnpinPageException,
      PageUnpinnedException, InvalidFrameNumberException, HashEntryNotFoundException,
      ReplacerException {
    try {
      this.columnarFile = new Columnarfile(relName);
      // f = new Heapfile(this.relName);
    } catch (Exception e) {
      throw new IndexException(e, "IndexScan.java: Heapfile not created");
    }

    for (IndexType curIndex : index) {
      this.BMFiles = this.generate_bitmap_file(curIndex, indName);
      this.distinctColPos = this.getMatchedPosition();

      this.scanIndex = 0;
      this.scannedTID = new ArrayList<TID>();
      this.tidHeapScanner = this.columnarFile.tidHeap.openScan();
    }
  }

  private BitMapFile[] generate_bitmap_file(IndexType indexType, String[] indexName)
      throws GetFileEntryException, PinPageException, ConstructPageException, HFDiskMgrException,
      IOException, cbitmap.GetFileEntryException, cbitmap.PinPageException,
      cbitmap.ConstructPageException, HFException, HFBufMgrException, InvalidSlotNumberException,
      UnpinPageException, bitmap.UnpinPageException, InvalidTupleSizeException {
    BitMapFile[] bitMapFiles = new BitMapFile[0];

    switch (indexType.indexType) {
      case IndexType.Bitmap:
        bitMapFiles = new BitMapFile[indexName.length];
        for (int i = 0; i < indexName.length; i++) {
          bitMapFiles[i] = new BitMapFile(indexName[i]);
        }
        break;

      case IndexType.CBitmap:
        bitMapFiles = new CBitMapFile[indexName.length];
        for (int i = 0; i < indexName.length; i++) {
          bitMapFiles[i] = new CBitMapFile(indexName[i]);
        }
        break;

      default:
        break;
    }

    return bitMapFiles;
  }

  private ArrayList<Integer> getMatchedPosition()
      throws HFDiskMgrException, GetFileEntryException, ConstructPageException, PinPageException,
      bitmap.UnpinPageException, IOException, PageUnpinnedException, InvalidFrameNumberException,
      HashEntryNotFoundException, ReplacerException {
    // Get all positions with data
    ArrayList<Integer> columnPositions = new ArrayList<>();
    for (int i = 0; i < this.BMFiles.length; i++) {
      columnPositions.addAll(this.BMFiles[i].getMatchedPosition());
      this.BMFiles[i].close();
    }
    // Remove duplicates
    ArrayList<Integer> matchedPosition = this.removeDuplicates(columnPositions);
    Collections.sort(matchedPosition);

    return matchedPosition;
  }

  /**
   * returns the next tuple. if <code>index_only</code>, only returns the key value (as the first
   * field in a tuple) otherwise, retrive the tuple and returns the whole tuple
   *
   * @return the tuple
   * @exception IndexException error from the lower layer
   * @exception UnknownKeyTypeException key type unknown
   * @exception IOException from the lower layer
   * @throws InvalidTupleSizeException
   */
  public Tuple get_next()
      throws IndexException, UnknownKeyTypeException, IOException, InvalidTupleSizeException {
    RID rid = new RID();
    Tuple tidTuple;
    byte[] byteArray;
    TID tid;
    boolean isLastChance = false;

    if (this.scanIndex < this.distinctColPos.size()) {
      // Traverse tidHeapFile
      while (true) {
        tidTuple = this.tidHeapScanner.getNext(rid);

        if (tidTuple == null) {
          if (isLastChance) {
            return null;
          }

          this.tidHeapScanner = this.columnarFile.tidHeap.openScan();
          isLastChance = true;
          continue;
        }

        try {
          // byteArray stores target byteArray
          byteArray = tidTuple.getTupleByteArray();
          tid = new TID(0, byteArray);
        } catch (Exception e) {
          throw new IndexException(e, "ColumnarIndexScan.java: getTID failed");
        }

        if (tid.position == this.distinctColPos.get(this.scanIndex)) {
          try {
            tuple1 = columnarFile.getTuple(tid);
            this.scannedTID.add(tid);
          } catch (Exception e) {
            throw new IndexException(e, "ColumnarIndexScan.java: getRecord failed");
          }

          this.scanIndex++;
          this.tidHeapScanner.closescan();
          return tuple1;
        }
      }
    }

    if (this.tidHeapScanner != null)
      this.tidHeapScanner.closescan();

    return null;
  }

  public void resetScanner() throws InvalidTupleSizeException, IOException {
    this.scanIndex = 0;
    this.tidHeapScanner.closescan();
    this.tidHeapScanner = this.columnarFile.tidHeap.openScan();
  }

  public TID[] getScanedTids() {
    return this.scannedTID.toArray(new TID[0]);
  }

  /**
   * Cleaning up the index scan, does not remove either the original relation or the index from the
   * database.
   *
   * @exception IndexException error from the lower layer
   * @exception IOException from the lower layer
   */
  public void close() throws IOException, IndexException {
    try {
      if (this.tidHeapScanner != null)
        this.tidHeapScanner.closescan();
    } catch (Exception e) {
      throw new IndexException(e, "BTree error in destroying index scan.");
    }
  }

  private ArrayList<Integer> removeDuplicates(ArrayList<Integer> arrayList) {
    Set<Integer> set = new HashSet<>(arrayList);

    return new ArrayList<>(set);
  }
}
