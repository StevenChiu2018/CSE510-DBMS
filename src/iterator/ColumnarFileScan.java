package iterator;

import bufmgr.*;
import columnar.TupleScan;
import diskmgr.*;
import global.*;
import heap.*;
import java.io.*;

/**
 * open a heapfile and according to the condition expression to get output file, call get_next to
 * get all tuples
 */
public class ColumnarFileScan extends Iterator {
  private AttrType[] _in1;
  private short in1_len;
  private short[] s_sizes;
  private Columnarfile columnarFile;
  private TupleScan scan;
  private Tuple tuple1;
  private Tuple Jtuple;
  private int t1_size;
  private int nOutFlds;
  private CondExpr[] OutputFilter;
  public FldSpec[] perm_mat;

  /**
   * constructor
   *
   * @param file_name heapfile to be opened
   * @param in1[] array showing what the attributes of the input fields are.
   * @param s1_sizes[] shows the length of the string fields.
   * @param len_in1 number of attributes in the input tuple
   * @param n_out_flds number of fields in the out tuple
   * @param proj_list shows what input fields go where in the output tuple
   * @param outFilter select expressions
   * @exception IOException some I/O fault
   * @exception FileScanException exception from this class
   * @exception TupleUtilsException exception from this class
   * @exception InvalidRelation invalid relation
   */
  public ColumnarFileScan(
      String file_name,
      AttrType in1[],
      short s1_sizes[],
      short len_in1,
      int n_out_flds,
      FldSpec[] proj_list,
      CondExpr[] outFilter)
      throws IOException, FileScanException, TupleUtilsException, InvalidRelation {
    this._in1 = in1;
    this.in1_len = len_in1;
    this.s_sizes = s1_sizes;

    this.Jtuple = new Tuple();
    AttrType[] Jtypes = new AttrType[n_out_flds];
    short[] ts_size;
    ts_size =
        TupleUtils.setup_op_tuple(
            this.Jtuple, Jtypes, in1, len_in1, s1_sizes, proj_list, n_out_flds);

    this.OutputFilter = outFilter;
    this.perm_mat = proj_list;
    this.nOutFlds = n_out_flds;
    this.tuple1 = new Tuple();

    try {
      this.tuple1.setHdr(this.in1_len, this._in1, s1_sizes);
    } catch (Exception e) {
      throw new FileScanException(e, "setHdr() failed");
    }
    this.t1_size = this.tuple1.size();

    try {
      this.columnarFile = new Columnarfile(file_name);

    } catch (Exception e) {
      throw new FileScanException(e, "Create new heapfile failed");
    }

    try {
      this.scan = columnarFile.openTupleScan();
    } catch (Exception e) {
      throw new FileScanException(e, "openScan() failed");
    }
  }

  /**
   * @return shows what input fields go where in the output tuple
   */
  public FldSpec[] show() {
    return perm_mat;
  }

  /**
   * @return the result tuple
   * @exception JoinsException some join exception
   * @exception IOException I/O errors
   * @exception InvalidTupleSizeException invalid tuple size
   * @exception InvalidTypeException tuple type not valid
   * @exception PageNotReadException exception from lower layer
   * @exception PredEvalException exception from PredEval class
   * @exception UnknowAttrType attribute type unknown
   * @exception FieldNumberOutOfBoundException array out of bounds
   * @exception WrongPermat exception for wrong FldSpec argument
   */
  public Tuple get_next()
      throws JoinsException,
          IOException,
          InvalidTupleSizeException,
          InvalidTypeException,
          PageNotReadException,
          PredEvalException,
          UnknowAttrType,
          FieldNumberOutOfBoundException,
          WrongPermat {
    TID tid = new TID();
    while (true) {
      if ((this.tuple1 = scan.getNext(tid)) == null) {
        return null;
      }
      this.tuple1.setHdr(this.in1_len, this._in1, this.s_sizes);
      if (PredEval.Eval(this.OutputFilter, this.tuple1, null, this._in1, null) == true) {
        Projection.Project(this.tuple1, this._in1, this.Jtuple, this.perm_mat, this.nOutFlds);
        return this.Jtuple;
      }
    }
  }

  /** implement the abstract method close() from super class Iterator to finish cleaning up */
  public void close() {

    if (!this.closeFlag) {
      this.scan.closescan();
      this.closeFlag = true;
    }
  }
}
