package index;

import btree.*;
import bufmgr.*;
import diskmgr.*;
import global.*;
import heap.*;
import iterator.*;
import java.io.*;
import java.util.ArrayList;

/**
 * Index Scan iterator will directly access the required tuple using
 * the provided key. It will also perform selections and projections.
 * information about the tuples and the index are passed to the constructor,
 * then the user calls <code>get_next()</code> to get the tuples.
 */
public class ColumnIndexScan extends 4Iterator {

  /**
   * class constructor. set up the ColumnIndexScan scan.
   * @param index type of the index (B_Index, Hash, Bitmap)
   * @param relName name of the input relation
   * @param indName name of the input index
   * @param types array of types in this relation
   * @param str_sizes array of string sizes (for attributes that are string)
   * @param outFlds fields to project
   * @param selects conditions to apply, first one is primary
   * @param indexOnly whether the answer requires only the key or the tuple
   * @exception IndexException error from the lower layer
   * @exception InvalidTypeException tuple type not valid
   * @exception InvalidTupleSizeException tuple size not valid
   * @exception UnknownIndexTypeException index type unknown
   * @exception IOException from the lower layer
   */
  public ColumnIndexScan(
    IndexType index,
    java.lang.String relName,
    java.lang.String indName,
    AttrType type,
    short str_sizes,
    CondExpr[] selects,
    boolean indexOnly
  )
    throws IndexException, InvalidTypeException, InvalidTupleSizeException, UnknownIndexTypeException, IOException {  
      _getNextIndex = 0;
    } catch (TupleUtilsException e) {
      throw new IndexException(
        e,
        "IndexScan.java: TupleUtilsException caught from TupleUtils.setup_op_tuple()"
      );
    } catch (InvalidRelation e) {
      throw new IndexException(
        e,
        "IndexScan.java: InvalidRelation caught from TupleUtils.setup_op_tuple()"
      );
    }

    try {
      f = new Heapfile(relName);
    } catch (Exception e) {
      throw new IndexException(e, "IndexScan.java: Heapfile not created");
    }

    switch (index.indexType) {
      // linear hashing is not yet implemented
      case IndexType.Bitmap:
        // error check the select condition
        // must be of the type: value op symbol || symbol op value
        // but not symbol op symbol || value op value
        try {
          indFile = new BitMapFile(indName);
        } catch (Exception e) {
          throw new IndexException(
            e,
            "IndexScan.java: BitmapFile exceptions caught from BitMapFile constructor"
          );
        }

        try {
          //indScan = (BTFileScan) IndexUtils.BTree_scan(selects, indFile);
          position = IndexUtils.Bitmap_scan(indFile,indName);
        } catch (Exception e) {
          throw new IndexException(
            e,
            "IndexScan.java: BTreeFile exceptions caught from IndexUtils.BTree_scan()."
          );
        }

        break;
      case IndexType.None:
      default:
        throw new UnknownIndexTypeException(
          "Only Bitmap index is supported so far"
        );
    }


  /**
   * returns the next tuple.
   * if <code>index_only</code>, only returns the key value
   * (as the first field in a tuple)
   * otherwise, retrive the tuple and returns the whole tuple
   * @return the tuple
   * @exception IndexException error from the lower layer
   * @exception UnknownKeyTypeException key type unknown
   * @exception IOException from the lower layer
   */
  public Tuple get_next() throws IndexException, UnknownKeyTypeException, IOException {
    int curposition = position.get(_getNextIndex);
    RID[] records = new RID[1];
    Tuple t = hf.getRecord(getRIDFromPosition(curposition, hf)); // get tid based on position

    return t;
  }

  /**
   * Cleaning up the index scan, does not remove either the original
   * relation or the index from the database.
   * @exception IndexException error from the lower layer
   * @exception IOException from the lower layer
   */
  public void close() throws IOException, IndexException {
    if (!closeFlag) {
      if (indScan instanceof BTFileScan) {
        try {
          ((BTFileScan) indScan).DestroyBTreeFileScan();
        } catch (Exception e) {
          throw new IndexException(e, "BTree error in destroying index scan.");
        }
      }

      closeFlag = true;
    }
  }
  public static RID getRIDFromPosition(int position, Heapfile hf)
      throws HFBufMgrException, IOException, InvalidSlotNumberException, InvalidTupleSizeException {
    int curcount = position;
    PageId currentDirPageId = new PageId(hf._firstDirPageId.pid);
    HFPage currentDirPage = new HFPage();
    PageId nextDirPageId = new PageId(0);

    Page pageinbuffer = new Page();

    boolean flag = true;

    RID recid = new RID();
    DataPageInfo dpinfo = new DataPageInfo();
    while (currentDirPageId.pid != hf.INVALID_PAGE && flag) {
      hf.pinPage(currentDirPageId, currentDirPage, false);

      Tuple atuple;
      for (recid = currentDirPage.firstRecord();
          recid != null;  // rid==NULL means no more record
          recid = currentDirPage.nextRecord(recid)) {
        atuple = currentDirPage.getRecord(recid);
        dpinfo = new DataPageInfo(atuple);

        if (curcount - dpinfo.recct >= 0) {
          curcount -= dpinfo.recct;
        } else if (curcount == 0) {
          flag = false;
          break;
        } else {
          flag = false;
          break;
        }
      }

      // ASSERTIONS: no more record
      // - we have read all datapage records on
      //   the current directory page.

      if (flag) {
        nextDirPageId = currentDirPage.getNextPage();
        hf.unpinPage(currentDirPageId, false /*undirty*/);
        currentDirPageId.pid = nextDirPageId.pid;
      }
    }
    //recid points to data page with the position

    HFPage currentDataPage = new HFPage();
    PageId currentDataPageId = new PageId(dpinfo.getPageId().pid);
    hf.pinPage(currentDataPageId, currentDataPage, false/*Rdisk*/);

    RID record = new RID();
    for (record = currentDataPage.firstRecord();
        record != null && curcount > 0;  // rid==NULL means no more record
        record = currentDataPage.nextRecord(record)) {
      curcount--;
    }
//        RID record = currentDataPage.firstRecord();
//        curcount--;
//        while( record != null && curcount>=0) {
//            record = currentDataPage.nextRecord(record);
//            curcount--;
//        }
    hf.unpinPage(currentDataPageId, false);

    return record;
  }


  private IndexFile indFile;
  private IndexFileScan indScan;
  private AttrType _type;
  private short[] _s_sizes;
  private CondExpr[] _selects;
  private Heapfile f;
  private Tuple tuple1;
  private Tuple Jtuple;
  private int t1_size;
  private boolean index_only;
  private ArrayList<Integer> position;
  private int _getNextIndex;

}
