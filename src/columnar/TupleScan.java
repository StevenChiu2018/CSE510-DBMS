package columnar;

/** JAVA */
/**
 * TupleScan.java- class Scan
 *
 */

import java.io.*;
import global.*;
import global.TID;
import global.AttrType;
import heap.Heapfile;
import heap.Scan;
import heap.Tuple;


public class TupleScan implements GlobalConst {


  public Scan[] scan;
  public ColumnarFile cf;

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

  public void closetuplescan() {

    for (Scan s: scan) {
      s.closescan();
    }

    scan = null;
  }

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

