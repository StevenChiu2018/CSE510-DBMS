package programs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import columnar.Columnarfile;
import global.SystemDefs;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.SpaceNotAvailableException;

/*
 * Query command format:
 *
 * query/delete_query
 *
 * use [:COLUMN_DB] with [:BUFFER_AMOUNT]
 *
 * from [:COLUMNARFILE_NAME]
 *
 * (join [:COLUMNARFILE_NAME] on [:COLUMN_NAMES] with [:JOIN_METHOD])
 *
 * select [:COLUMN_NAMES]
 *
 * (where [:CONSTRAINTS])
 *
 * (scan_with [:SCAN_METHOD])
 */
public class QueryParams {
  public boolean doDelete;
  public SystemDefs DB;
  public Columnarfile baseColumnarFile;
  public ArrayList<QueryColumnInfo> queryColumns;
  public ArrayList<QueryColumnInfo> selectedColumns;
  public Constraint whereConstraint;
  public String scanMethod;

  public QueryParams(String[] commands)
      throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
      SpaceNotAvailableException, InvalidSlotNumberException, IOException {
    this.doDelete = false;
    this.DB = null;
    this.baseColumnarFile = null;
    this.queryColumns = new ArrayList<QueryColumnInfo>();
    this.selectedColumns = new ArrayList<QueryColumnInfo>();
    this.whereConstraint = null;
    this.scanMethod = "";

    for (int i = 0; i < commands.length;) {
      switch (commands[i]) {
        case "delete_query":
          this.doDelete = true;

          i += 1;
          break;
        case "use":
          this.DB = new SystemDefs(commands[i + 1], 0, Integer.parseInt(commands[i + 3]), null);

          i += 4;
          break;
        case "from":
          this.baseColumnarFile = new Columnarfile(commands[i + 1]);
          this.addColumnsFrom(this.baseColumnarFile);

          i += 2;
          break;

        case "select":
          if (commands[i + 1].equals("*")) {
            this.selectedColumns = new ArrayList<QueryColumnInfo>(this.queryColumns);
          } else {
            String[] columnNames = commands[i + 1].split(",");
            this.createSelectedColumns(columnNames);
          }

          i += 2;
          break;

        case "where":
          this.whereConstraint = new Constraint(commands[i + 1], this.queryColumns);

          i += 2;
          break;

        case "scan_with":
          this.scanMethod = commands[i + 1];

          i += 2;
          break;

        default:
          i += 1;
          break;
      }
    }
  }

  private void addColumnsFrom(Columnarfile columnarfile) {
    int at = 0;
    if (this.queryColumns.size() != 0) {
      QueryColumnInfo lastColumn = this.queryColumns.get(this.queryColumns.size() - 1);
      at = lastColumn.tupleOffset + lastColumn.columnInfo.sizeInBytes;
    }

    for (int i = 0; i < columnarfile.columnsInfo.length; i++) {
      String columnName = QueryColumnInfo.columnName(columnarfile, i);
      QueryColumnInfo queryColumn =
          new QueryColumnInfo(columnName, at, columnarfile.columnsInfo[i]);

      this.queryColumns.add(queryColumn);
      at += columnarfile.columnsInfo[i].sizeInBytes;
    }
  }

  private void createSelectedColumns(String[] columnNames) {
    HashSet<String> columnNameSet = new HashSet<String>();
    for (int i = 0; i < columnNames.length; i++)
      columnNameSet.add(columnNames[i]);

    Iterator<QueryColumnInfo> columnsIterator = this.queryColumns.iterator();
    while (columnsIterator.hasNext()) {
      QueryColumnInfo cur = columnsIterator.next();

      if (columnNameSet.contains(cur.name)) {
        this.selectedColumns.add(cur);
      }
    }
  }
}
