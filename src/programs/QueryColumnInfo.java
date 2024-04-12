package programs;

import columnar.ColumnInfo;
import columnar.Columnarfile;

public class QueryColumnInfo {
  public String name;
  public int tupleOffset;
  public ColumnInfo columnInfo;

  public QueryColumnInfo() {
    this.name = "";
    this.tupleOffset = 0;
    this.columnInfo = null;
  }

  public QueryColumnInfo(String name, int offset, ColumnInfo columnInfo) {
    this.name = name;
    this.tupleOffset = offset;
    this.columnInfo = columnInfo;
  }

  public static QueryColumnInfo copied(QueryColumnInfo info) {
    QueryColumnInfo newInfo = new QueryColumnInfo();
    newInfo.name = info.name;
    newInfo.tupleOffset = info.tupleOffset;
    newInfo.columnInfo = info.columnInfo;

    return newInfo;
  }

  public static String columnName(Columnarfile columnarfile, int columnIndex) {
    return columnarfile.name + "." + columnarfile.columnsInfo[columnIndex].columnName;
  }
}
