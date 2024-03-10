import bufmgr.*;
import diskmgr.*;
import global.*;
import heap.*;
import java.io.*;

class Columnarfile {
  private int numColumns;
  private AttrType[] type;
  private Heapfile[] heapfiles;
  private String name;

  public Columnarfile(String name, int numColumns, AttrType[] type, int stringSize)
      throws IOException,
          HFException,
          HFBufMgrException,
          HFDiskMgrException,
          SpaceNotAvailableException,
          InvalidSlotNumberException,
          InvalidTupleSizeException {
    this.numColumns = numColumns;
    this.type = type;
    this.name = name;
    this.heapfiles = new Heapfile[numColumns];

    if (!isFileExist(name + ".hdr")) {
      // create
      createHeaderFile();
      for (int i = 0; i < numColumns; i++) {
        heapfiles[i] = new Heapfile(name + "." + i);
      }
    } else {
      // load
      loadHeaderFile();
    }
  }

  private PageId get_file_entry(String filename) throws HFDiskMgrException {

    PageId tmpId = new PageId();

    try {
      tmpId = SystemDefs.JavabaseDB.get_file_entry(filename);
    } catch (Exception e) {
      throw new HFDiskMgrException(e, "Heapfile.java: get_file_entry() failed");
    }

    return tmpId;
  }

  private boolean isFileExist(String name) {
    try {
      PageId pageid = get_file_entry(name);
      return pageid != null && pageid.pid > 0;
    } catch (Exception e) {
      return false;
    }
  }

  private void loadHeaderFile()
      throws HFDiskMgrException,
          HFException,
          HFBufMgrException,
          IOException,
          InvalidTupleSizeException {
    Heapfile headerfile = new Heapfile(this.name + ".hdr");
    Scan sc = headerfile.openScan();
    RID rid = new RID();
    Tuple tuple;
    if ((tuple = sc.getNext(rid)) != null) {
      byte[] data = tuple.getTupleByteArray();
      numColumns = Convert.getIntValue(0, data);
      this.type = new AttrType[numColumns];
      for (int i = 0; i < numColumns; i++) {
        int attrType = Convert.getIntValue(4 + i * 4, data);
        this.type[i] = new AttrType(attrType);
      }
    }

    sc.closescan();
    this.heapfiles = new Heapfile[numColumns];
    for (int i = 0; i < numColumns; i++) {
      this.heapfiles[i] = new Heapfile(this.name + "." + i);
    }
  }

  private void createHeaderFile()
      throws IOException,
          HFDiskMgrException,
          HFException,
          HFBufMgrException,
          SpaceNotAvailableException,
          InvalidSlotNumberException,
          InvalidTupleSizeException {
    Heapfile hdrf = new Heapfile(this.name + ".hdr");
    byte[] metaByte = new byte[4 + 4 * numColumns];
    Convert.setIntValue(numColumns, 0, metaByte);
    for (int i = 0; i < numColumns; i++) {
      Convert.setIntValue(type[i].attrType, 4 + i * 4, metaByte);
    }

    hdrf.insertRecord(metaByte);
  }

  // Return the number of tuples in the columnar file.
  public int getTupleCnt()
      throws InvalidSlotNumberException, InvalidTupleSizeException, IOException {

    int count = 0;
    try {
      count = heapfiles[0].getRecCnt();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return count;
  }

  // Initiate a sequential scan of tuples.
  public TupleScan openTupleScan() {
    TupleScan scan = new TupleScan(this);
    return scan;
  }

  // Initiate a sequential scan along a given column.
  public Scan openColumnScan(int columnNo) {
    Scan scan = null;
    try {
      scan = new Scan(heapfiles[columnNo]);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return scan;
  }

  // Update the specified record in the columnar file.
  public boolean updateTuple(TID tid, Tuple newtuple) {
    for (int i = 0; i < numColumns; i++) {
      if (updateColumnofTuple(tid, newtuple, i + 1) == false) {
        return false;
      }
    }
    return true;
  }

  // Update the specified column of the specified record in the columnar file.
  public boolean updateColumnofTuple(TID id, Tuple newtuple, int column) {
    int intValue;
    String strValue;
    Tuple tuple = null;
    try {
      if (type[column - 1].attrType == AttrType.attrInteger) {
        intValue = newtuple.getIntFld(column);
        tuple = new Tuple(4);
        tuple.setIntFld(1, intValue);
      } else if (type[column - 1].attrType == AttrType.attrString) {
        strValue = newtuple.getStrFld(column);
        tuple = new Tuple(stringSize);
        tuple.setStrFld(1, strValue);
      }

      return heapfiles[column - 1].updateRecord(tid.recordIDs[column - 1], tuple);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}