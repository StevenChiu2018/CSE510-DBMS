import bufmgr.*;
import diskmgr.*;
import global.*;
import heap.*;
import java.io.*;
import ColumnInfo;

class Columnarfile {
  private int numColumns;
  private AttrType[] type;
  private Heapfile[] columns;
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
    this.columns = new Heapfile[numColumns];

    if (!isFileExist(name + ".hdr")) {
      // create
      createHeaderFile();
      for (int i = 0; i < numColumns; i++) {
        columns[i] = new Heapfile(name + "." + i);
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
    this.columns = new Heapfile[numColumns];
    for (int i = 0; i < numColumns; i++) {
      this.columns[i] = new Heapfile(this.name + "." + i);
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

  // Read the tuple with the given tid from the columnar file
  public Tuple getTuple(TID tid) {
    byte[] tuple = new byte[tupleLength];
    int offset = 0;
    int length = 0;

    Tuple t = new Tuple();

    try {
      for (int i = 0; i < numColumns ; i++) {

        t = columns[i].getRecord(tid.recordIDs[i]);

        if (type[i].attrType == AttrType.attrInteger) {				
          int value = Convert.getIntValue(offset, t.returnTupleByteArray());
          Convert.setIntValue(value, offset, tuple);
          offset = offset + 4;
          length += 4;
        }

        if (type[i].attrType == AttrType.attrString) {
          String value = Convert.getStrValue(offset, t.returnTupleByteArray(), columnsInfo[i].sizeInBytes);
          Convert.setStrValue(value, offset, tuple);
          offset = offset + columnsInfo[i].sizeInBytes;
          length += columnsInfo[i].sizeInBytes;
        }
      }
      t.tupleSet(tuple, 0, length);


    } catch (Exception e) {
      e.printStackTrace();
    }
    return t;
  }

  // Read the value with the given column and  tid from the columnar file
  public ValueClass getValue(TID tid, int column) {

    ValueClass value = null;
		IntegerValue integer = new IntegerValue();
		StringValue str = new StringValue();

		try{
			byte[] colValue = columns[column].getRecord(tid.recordIDs[column]).returnTupleByteArray();

			if (type[column].attrType == AttrType.attrInteger)	{

				integer.setValue(Convert.getIntValue(0, colValue));
				value = integer;
			}
			else if (type[column].attrType == AttrType.attrString)	{

				str.setValue(Convert.getStrValue(0, colValue, columnsInfo[column].sizeInBytes));
				value = str;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return value;
  }

  // Return the number of tuples in the columnar file.
  public int getTupleCnt()
      throws InvalidSlotNumberException, InvalidTupleSizeException, IOException {

    int count = 0;
    try {
      count = columns[0].getRecCnt();
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
      scan = new Scan(columns[columnNo]);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return scan;
  }

  // Update the specified record in the columnar file.
  public boolean updateTuple(TID tid, Tuple newRowTuple) {
    int offset = 0;
    byte[] columnByteArray;
    byte[] byteArray = newRowTuple.getTupleByteArray();

    for (int i = 0; i < numColumns; i++) {
      columnByteArray = new byte[columnsInfo[i].sizeInBytes];
      System.arraycopy(byteArray, offset, columnByteArray, 0, columnsInfo[i].sizeInBytes);
      offset += columnsInfo[i].sizeInBytes;

      Tuple newTuple = new Tuple(columnByteArray, 0, columnsInfo[i].sizeInBytes);
      if (updateColumnofTuple(tid, newTuple, i) == false) {
        return false;
      }
    }
    
    return true;
  }

  // Update the specified column of the specified record in the columnar file.
  public boolean updateColumnofTuple(TID tid, Tuple newtuple, int column) {
    int intValue;
    String strValue;
    Tuple tuple = null;
    try {
      if (type[column].attrType == AttrType.attrInteger) {
        intValue = newtuple.getIntFld(column);
        tuple = new Tuple(4);
        tuple.setIntFld(1, intValue);
      } else if (type[column].attrType == AttrType.attrString) {
        strValue = newtuple.getStrFld(column);
        tuple = new Tuple(ColumnsInfo[column].sizeInBytes);
        tuple.setStrFld(1, strValue);
      }

      return columns[column].updateRecord(tid.recordIDs[column], tuple);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}