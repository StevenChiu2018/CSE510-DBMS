package columnar;

/** JAVA */
/**
 * TupleScan.java- class Scan
 *
 */

import java.io.*;
import global.*;
import global.TID;
import heap.Heapfile;
import heap.Scan;
import heap.Tuple;


public class TupleScan implements GlobalConst {


  public Scan[] scan;
  public ColumnarFile cf;

  /**
   * The constructor pins the first directory page in the file and initializes its private data
   * members from the private data member from cf
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   *
   * @param cf A ColumnarFile object
   */
  public TupleScan(Columnarfile cf) {

    int i = 0;
    this.cf = cf;
    this.scan = new Scan[cf.numColumns];

    try {
      for (Heapfile hf: cf.heapfiles) {
        scan[i] = hf.openScan();
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /** Closes the TupleScan object */
  public void closetuplescan() {

    for (Scan s: scan) {
      s.closescan();
    }

    scan = null;
  }

  /**
   * Retrieve the next tuple in a sequential scan
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   *
   * @param tid Tuple ID of the record
   * @return the Tuple of the retrieved tuple.
   */
  public Tuple getNext(TID tid) {

    Tuple tuple = new Tuple(cf.tupleLength);
    short[] fieldsOffset = new short[cf.getTupleCnt()];

    for (int i = 0, offset = 0; i < cf.numColumns; i++) {

      if(cf.attributeType[i].attrType == AttrType.attrInteger) {
        fieldsOffset[i] = (short) offset;
        offset = offset + 4;
      }
      if (cf.attributeType[i].atttrType == AttrType.attrString) {
        fieldsOffset[i] = (short) offset;
        offset += offset + cf.stringSize;
      }
    }

    try {
      tuple.setTupleMetaData(cf.tupleLength, (short)cf.numColumns, fieldsOffset);
      int i = 0;

      for (Scan hf: scan) {
        if(cf.attributeType[i].attrType == AttrType.attrInteger) {
          Tuple t = hf.getNext(tid.recordIDs[i]);
          if(t == null) {
            return null;
          }
          t.setTupleMetaData(4, (short)1, fieldsOffset);
          tuple.setIntFld(i + 1, t.getIntFld(1));
        }

        if(cf.attributeType[i].attrType == AttrType.attrString) {
          Tuple t = hf.getNext(tid.recordIDs[i]);
          if(t == null) {
            return null;
          }
          t.setTupleMetaData(cf.stringSize, (short)1, fieldsOffset);
          tuple.setIntFld(i + 1, t.getIntFld(1));
        }
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return tuple;
  }


  /**
   * Position all scans cursors to the records with the given tid.
   *
   * @exception InvalidTupleSizeException Invalid tuple size
   * @exception IOException I/O errors
   * @param tid Record ID of the given record
   * @return true if successful, false otherwise.
   */
  public boolean position(TID tid) {

    int i = 0;

    try {
      for (Scan s: this.scan) {
        if(!s.position(tid.recordIDs[i])) {
          i++;
        }
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;

  }

}

