package index;

import btree.*;
import bufmgr.*;
import diskmgr.*;
import global.*;
import heap.*;
import iterator.*;
import columnar.Columnarfile;
import java.io.*;
import java.util.ArrayList;

/**
 * Index Scan iterator will directly access the required tuple using
 * the provided key. It will also perform selections and projections.
 * information about the tuples and the index are passed to the constructor,
 * then the user calls <code>get_next()</code> to get the tuples.
 */
public class ColumnIndexScan extends Iterator {

  /**
   * class constructor. set up the ColumnIndexScan scan.
   * @param index type of the index (B_Index, Hash, Bitmap)
   * @param relName name of the input relation
   * @param indName name of the input index
   * @param type types of this column
   * @param str_sizes array of string sizes (for attributes that are string)
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

    try {
      hf = new Heapfile(relName);
      cf = new Columnarfile(relName);
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
      default:
        throw new UnknownIndexTypeException(
          "Only Bitmap index is supported so far"
        );
    }
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
    _getNextIndex+=1;

    //scan columnar file
    Scan scan = cf.tidheap.openScan();
    RID rid = new RID();
    Tuple tuple = scan.getNext(rid);
    if(tuple!=null){
      TID tid = new TID(0,tuple.getTupleByteArray());
      if(tid.position == curposition)return cf.getTuple(tid);
    }

    //RID[] records = new RID[1];
    //Tuple t = hf.getRecord(getRIDFromPosition(curposition, hf)); // get tid based on position


    return null;
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


  private IndexFile indFile;
  private IndexFileScan indScan;
  private Heapfile hf;
  private Columnarfile cf;
  private ArrayList<Integer> position;
  private int _getNextIndex;

}
