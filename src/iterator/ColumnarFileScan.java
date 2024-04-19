package iterator;

import bufmgr.*;
import columnar.Columnarfile;
import columnar.TupleScan;
import diskmgr.*;
import global.*;
import heap.*;
import java.io.*;

/**
 * open a heapfile and according to the condition expression to get output file, call get_next to
 * get all tuples
 */
public class ColumnarFileScan {
  private Columnarfile columnarFile;
  private TupleScan scanner;

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
  public ColumnarFileScan(String file_name)
      throws IOException, FileScanException, TupleUtilsException, InvalidRelation {
    try {
      this.columnarFile = new Columnarfile(file_name);

    } catch (Exception e) {
      throw new FileScanException(e, "Create new heapfile failed");
    }

    try {
      this.scanner = columnarFile.openTupleScan();
    } catch (Exception e) {
      e.printStackTrace();
      throw new FileScanException(e, "openScan() failed");
    }
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
  public Tuple get_next(TID tid) throws JoinsException, IOException, InvalidTupleSizeException,
      InvalidTypeException, PageNotReadException, PredEvalException, UnknowAttrType,
      FieldNumberOutOfBoundException, WrongPermat {
    Tuple result;
    while ((result = scanner.getNext(tid)) != null) {
      return result;
    }

    return null;
  }

  /** implement the abstract method close() from super class Iterator to finish cleaning up */
  public void close() {
    this.scanner.closescan();
  }
}
