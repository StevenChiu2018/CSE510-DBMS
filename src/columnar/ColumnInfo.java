package columnar;

import global.*;

import java.io.IOException;

public class ColumnInfo {
    public String columnName;
    public AttrType type;
    public int sizeInBytes;
    public int columnNo;
    public String fileName;

    public ColumnInfo(byte[] byteArray) throws IOException {
        int offset = 0;
        this.columnNo = Convert.getIntValue(offset, byteArray);
        offset += 4;
        this.columnName = Convert.getStrValue(offset, byteArray, 100);
        offset += 100;
        this.type = new AttrType(Convert.getIntValue(offset, byteArray));
        offset += 4;
        this.fileName= Convert.getStrValue(offset, byteArray, 100);
        offset+=100;
        this.sizeInBytes = Convert.getIntValue(offset, byteArray);
    }

    public void writeToByteArray(byte[] byteArray, int offset) throws IOException {
        Convert.setIntValue(this.columnNo, offset, byteArray);
        offset += 4;
        Convert.setStrValue(this.columnName, offset, byteArray);
        offset+=100;
        Convert.setIntValue(this.type.attrType, offset, byteArray);
        offset += 4;
        Convert.setStrValue(this.fileName, offset, byteArray);
        offset += 100;
        Convert.setIntValue(this.sizeInBytes, offset, byteArray);


    }
    public int calculateSpace(){
        return 208;
    }


}
