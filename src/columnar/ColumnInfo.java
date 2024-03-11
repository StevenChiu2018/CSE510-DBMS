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

        if(byteArray == null || byteArray.length<offset+ calculateSpace()){
            throw new IllegalArgumentException("not enough space or byte array is null");
        }
        Convert.setIntValue(this.columnNo,offset,byteArray);
        offset+=Integer.BYTES;

        byte[] colNameBytes = this.columnName.getBytes();
        Convert.setIntValue(colNameBytes.length,offset,byteArray);
        offset+=Integer.BYTES;
        System.arraycopy(colNameBytes,0,byteArray,offset,colNameBytes.length);
        offset+=colNameBytes.length;

        Convert.setIntValue(this.type.attrType,offset, byteArray);
        offset+=Integer.BYTES;

        byte[] filenameBytes = this.fileName.getBytes();
        Convert.setIntValue(filenameBytes.length,offset,byteArray);
        offset+=Integer.BYTES;
        System.arraycopy(filenameBytes,0,byteArray,offset,filenameBytes.length);
        //offset+=filenameBytes.length;


    }
    public int calculateSpace(){
        return Integer.BYTES+ // colu No.
                Integer.BYTES+this.columnName.getBytes().length+ // name and length
                Integer.BYTES+ // type
                Integer.BYTES+this.fileName.getBytes().length; //filename and len
    }


}
