package programs;

import columnar.ColumnInfo;
import columnar.Columnarfile;

public class QueryColumnInfo {
  public String name;
  public int tupleOffset;
  public ColumnInfo columnInfo;

  public QueryColumnInfo(String name, int offset, ColumnInfo columnInfo) {
    this.name = name;
    this.tupleOffset = offset;
    this.columnInfo = columnInfo;
  }

  public static String columnName(Columnarfile columnarfile, int columnIndex) {
    return columnarfile.name + "." + columnarfile.columnsInfo[columnIndex].columnName;
  }
}
