package columnar;

import global.AttrType;

public class ColumnInfo {
    public String name;
    public AttrType type;
    public int sizeInBytyes;

    public ColumnInfo(String name, AttrType type, int sizeInBytes){
        this.name = name;
        this.type = type;
        this.sizeInBytyes = sizeInBytes;
    }
}
