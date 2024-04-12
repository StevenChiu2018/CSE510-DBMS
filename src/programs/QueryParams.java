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
  public Columnarfile joinedColumnarFile;
  public ArrayList<QueryColumnInfo> queryColumns;
  public ArrayList<QueryColumnInfo> selectedColumns;
  public ArrayList<QueryColumnInfo> joinedColumns;
  public Constraint whereConstraint;
  public String scanMethod;
  public String joinMethod;

  public QueryParams(String[] commands)
      throws HFDiskMgrException, HFException, HFBufMgrException, InvalidTupleSizeException,
      SpaceNotAvailableException, InvalidSlotNumberException, IOException {
    this.doDelete = false;
    this.DB = null;
    this.baseColumnarFile = null;
    this.joinedColumnarFile = null;
    this.queryColumns = new ArrayList<QueryColumnInfo>();
    this.selectedColumns = new ArrayList<QueryColumnInfo>();
    this.joinedColumns = new ArrayList<QueryColumnInfo>();
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
          // My print
          System.out.println(commands[i + 1].length());
          this.baseColumnarFile = new Columnarfile(commands[i + 1]);
          this.addColumnsFrom(this.baseColumnarFile);

          i += 2;
          break;

        case "join":
          this.joinedColumnarFile = new Columnarfile(commands[i + 1]);
          this.addColumnsFrom(this.joinedColumnarFile);
          this.parseJoinedColumns(commands[i + 3]);
          this.joinMethod = commands[i + 5];

          i += 6;
          break;

        case "select":
          if (commands[i + 1].equals("*")) {
            this.selectedColumns = new ArrayList<QueryColumnInfo>();
            for (int j = 0; j < this.queryColumns.size(); j++)
              this.selectedColumns.add(QueryColumnInfo.copied(this.queryColumns.get(j)));
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
          new QueryColumnInfo(columnName, at, columnarfile.columnsInfo[i], i);

      this.queryColumns.add(queryColumn);
      at += columnarfile.columnsInfo[i].sizeInBytes;
    }
  }

  private void parseJoinedColumns(String command) {
    String[] parsedCommand = command.split("=");

    QueryColumnInfo left = this.getQueryColumnInfo(parsedCommand[0]);
    QueryColumnInfo right = this.getQueryColumnInfo(parsedCommand[1]);

    if (left.columnIndex < right.columnIndex) {
      this.joinedColumns.add(left);
      this.joinedColumns.add(right);
    } else {
      this.joinedColumns.add(right);
      this.joinedColumns.add(left);
    }
  }

  private QueryColumnInfo getQueryColumnInfo(String name) {
    for (int i = 0; i < this.queryColumns.size(); i++) {
      if (this.queryColumns.get(i).name.equals(name)) {
        return this.queryColumns.get(i);
      }
    }

    return null;
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
