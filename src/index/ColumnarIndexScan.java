package index;

import btree.*;
import bufmgr.*;
import columnar.Columnarfile;
import diskmgr.*;
import global.*;
import heap.*;
import iterator.*;
import java.io.*;
import bitmap.BitMapFile;
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
  private AttrType[] _types;
  private short[] _s_sizes;
  private CondExpr[] _selects;
  private int _noInFlds;
  private int _noOutFlds;
  private Heapfile f;
  private Columnarfile columnarFile;
  private Tuple tuple1;
  private Tuple Jtuple;
  private int t1_size;
  private int[] _fldNum;
  private boolean index_only;
  private int scanIndex;
  private Scan tidHeapScanner;
  private ArrayList<Integer> distinctColPos;
  private ArrayList<TID> scannedTID;


  /**
   * class constructor. set up the index scan.
   *
   * @param relName name of the input relation
   * @param fldNum array of field number of the indexed field
   * @param index array of type of the index (B_Index, Hash)
   * @param indName array of name of the input index
   * @param types array of types in this relation
   * @param str_sizes array of string sizes (for attributes that are string)
   * @param noInFlds number of fields in input tuple
   * @param noOutFlds number of fields in output tuple
   * @param outFlds fields to project
   * @param selects conditions to apply, first one is primary
   * @param indexOnly whether the answer requires only the key or the tuple
   * @exception IndexException error from the lower layer
   * @exception InvalidTypeException tuple type not valid
   * @exception InvalidTupleSizeException tuple size not valid
   * @exception UnknownIndexTypeException index type unknown
   * @exception IOException from the lower layer
   */
  public ColumnarIndexScan(final String relName, final int[] fldNum, IndexType[] index,
      final String[] indName, AttrType types[], short str_sizes[], int noInFlds, int noOutFlds,
      FldSpec outFlds[], CondExpr selects[], final boolean indexOnly) throws IndexException,
      InvalidTypeException, InvalidTupleSizeException, UnknownIndexTypeException, IOException {
    this._fldNum = fldNum;
    this._noInFlds = noInFlds;
    this._types = types;
    this._s_sizes = str_sizes;

    AttrType[] Jtypes = new AttrType[noOutFlds];
    short[] ts_sizes;
    this.Jtuple = new Tuple();

    this._selects = selects;
    this.perm_mat = outFlds;
    this._noOutFlds = noOutFlds;
    this.index_only = indexOnly; // added by bingjie miao

    try {
      this.columnarFile = new Columnarfile(relName);
      // f = new Heapfile(this.relName);
    } catch (Exception e) {
      throw new IndexException(e, "IndexScan.java: Heapfile not created");
    }

    for (IndexType curIndex : index) {
      switch (curIndex.indexType) {
        // Only bitmap is implemented
        case IndexType.BitMap:
          // error check the select condition
          // must be of the type: value op symbol || symbol op value
          // but not symbol op symbol || value op value
          try {
            this.BMFiles = new BitMapFile[indName.length];
            for (int i = 0; i < indName.length; i++) {
              System.out.println(indName[i]);
              this.BMFiles[i] = new BitMapFile(indName[i]);
            }
          } catch (Exception e) {
            throw new IndexException(e,
                "IndexScan.java: BitmapFile exceptions caught from BitmapFile constructor");
          }

          try {
            // Get all positions with data
            ArrayList<Integer> columnPositions = new ArrayList<>();
            for (int i = 0; i < this.BMFiles.length; i++) {
              columnPositions.addAll(IndexUtils.Bitmap_scan(this.BMFiles[i]));
              this.BMFiles[i].close();
            }
            // Remove duplicates
            this.distinctColPos = this.removeDuplicates(columnPositions);
            Collections.sort(this.distinctColPos);

            this.scanIndex = 0;
          } catch (Exception e) {
            throw new IndexException(e, "IndexScan.java: BTreeFile exceptions caught.");
          }

          this.scannedTID = new ArrayList<TID>();

          break;
        default:
          throw new UnknownIndexTypeException("Only BTree index is supported so far");
      }
    }
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

    if (this.scanIndex < this.distinctColPos.size()) {
      // Traverse tidHeapFile
      this.tidHeapScanner = columnarFile.tidHeap.openScan();
      while ((tidTuple = this.tidHeapScanner.getNext(rid)) != null) {
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

  public TID[] getScanneTids() {
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
