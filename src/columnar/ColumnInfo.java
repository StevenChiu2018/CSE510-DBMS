package columnar;

import global.*;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.Heapfile;
import java.io.IOException;

public class ColumnInfo {
    public String columnName;
    public AttrType type;
    public int sizeInBytes;
    public int columnNo;
    public String fileName;
    public Heapfile bitmapFileName;

    public ColumnInfo() {}

    public ColumnInfo(byte[] byteArray)
            throws IOException, HFException, HFBufMgrException, HFDiskMgrException {
        int offset = 0;
        this.columnNo = Convert.getIntValue(offset, byteArray);
        offset += 4;
        this.columnName = Convert.getStrValue(offset, byteArray, 100);
        offset += 100;
        this.type = new AttrType(Convert.getIntValue(offset, byteArray));
        offset += 4;
        this.fileName = Convert.getStrValue(offset, byteArray, 100);
        offset += 100;
        this.sizeInBytes = Convert.getIntValue(offset, byteArray);
        offset += 4;
        String bitmapFileNameFileName = Convert.getStrValue(offset, byteArray, 100);
        this.bitmapFileName = new Heapfile(bitmapFileNameFileName);
    }

    public void writeToByteArray(byte[] byteArray, int offset) throws IOException {
        Convert.setIntValue(this.columnNo, offset, byteArray);
        offset += 4;
        Convert.setStrValue(this.columnName, offset, byteArray);
        offset += 100;
        Convert.setIntValue(this.type.attrType, offset, byteArray);
        offset += 4;
        Convert.setStrValue(this.fileName, offset, byteArray);
        offset += 100;
        Convert.setIntValue(this.sizeInBytes, offset, byteArray);
        offset += 4;
        Convert.setStrValue(fileName + ".bitmapFileName", offset, byteArray);
    }

    public int calculateSpace() {
        return 312;
    }


}
