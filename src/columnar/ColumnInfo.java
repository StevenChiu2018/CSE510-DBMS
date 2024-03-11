package columnar;

import global.*;

import java.io.IOException;

public class ColumnInfo {
    public String columnName;
    public AttrType type;
    public int sizeInBytes;
    public int columnNo;
    public String fileName;

    public ColumnInfo(String columnName, AttrType type, int sizeInBytes,int columnNo,String columnarFilename){
        this.columnName = columnName;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.columnNo = columnNo;

        this.fileName = columnarFilename+'-'+columnName;
    }

    public void writeToByteArray(byte[] byteArray, int offset) throws IOException {

        Convert.setIntValue(this.columnNo,offset,byteArray);
        offset+=Integer.BYTES;

        Convert.setStrValue(this.columnName, offset, byteArray);
        offset += 100;

        Convert.setIntValue(this.type.attrType,offset, byteArray);
        offset+=Integer.BYTES;

        Convert.setStrValue(this.columnName, offset, byteArray);


    }
    public int calculateSpace(){
        return Integer.BYTES+ // colu No.
                Integer.BYTES+this.columnName.getBytes().length+ // name and length
                Integer.BYTES+ // type
                Integer.BYTES+this.fileName.getBytes().length; //filename and len
    }


}
