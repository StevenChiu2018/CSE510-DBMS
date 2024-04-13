package programs;

import columnar.ColumnInfo;
import columnar.Columnarfile;

public class QueryColumnInfo {
  public String name;
  public int tupleOffset;
  public ColumnInfo columnInfo;
  public int columnIndex;

  public QueryColumnInfo() {
    this.name = "";
    this.tupleOffset = 0;
    this.columnInfo = null;
    this.columnIndex = 0;
  }

  public QueryColumnInfo(String name, int offset, ColumnInfo columnInfo, int columnIndex) {
    this.name = name;
    this.tupleOffset = offset;
    this.columnInfo = columnInfo;
    this.columnIndex = columnIndex;
  }

  public static QueryColumnInfo copied(QueryColumnInfo info) {
    QueryColumnInfo newInfo = new QueryColumnInfo();
    newInfo.name = info.name;
    newInfo.tupleOffset = info.tupleOffset;
    newInfo.columnInfo = info.columnInfo;
    newInfo.columnIndex = info.columnIndex;

    return newInfo;
  }

  public static String columnName(Columnarfile columnarfile, int columnIndex) {
    return columnarfile.name + "." + columnarfile.columnsInfo[columnIndex].columnName;
  }
}
