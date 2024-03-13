package programs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import columnar.*;
import global.AttrType;
import global.Convert;
import global.SystemDefs;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.SpaceNotAvailableException;

class InsertedTable {
    public ColumnInfo[] header;
    public String[] rows;
    public int numColumns;
    public String tableName;

    public InsertedTable(String[] rawRows, int numColumns, String tableName) {
        this.header = new ColumnInfo[numColumns];
        StringTokenizer columnInfoTokenizer = new StringTokenizer(rawRows[0], "\t\r");
        for (int i = 0; i < numColumns; i++) {
            this.header[i] = constructColumnInfo(columnInfoTokenizer.nextToken());
        }

        this.rows = new String[rawRows.length - 1];
        this.numColumns = numColumns;
        this.tableName = tableName;

        for (int i = 1; i < rawRows.length; i++) {
            this.rows[i - 1] = rawRows[i];
        }
    }

    private ColumnInfo constructColumnInfo(String stringColumnInfo) {
        ColumnInfo columnInfo = new ColumnInfo();
        StringTokenizer columnTokenizer;

        columnTokenizer = new StringTokenizer(stringColumnInfo, ":");
        columnInfo.columnName = columnTokenizer.nextToken();
        columnInfo.fileName = this.tableName + "-column-info";

        columnTokenizer = new StringTokenizer(columnTokenizer.nextToken(), "(");
        if (columnTokenizer.nextToken().equals("char")) {
            columnInfo.type = new AttrType("attrString");
            columnTokenizer = new StringTokenizer(columnTokenizer.nextToken(), ")");
            columnInfo.sizeInBytes = Integer.parseInt(columnTokenizer.nextToken());
        } else {
            columnInfo.type = new AttrType("attrInteger");
            columnInfo.sizeInBytes = 4;
        }

        return columnInfo;
    }

    public int rowSizeInByte() {
        int size = 0;
        for (ColumnInfo column : this.header) {
            size += column.sizeInBytes;
        }

        return size;
    }
}


public class BatchInsert {
    public static void main(String[] args) throws Exception {
        if (!isValidInput(args)) {
            System.out.println(
                    "batchinsert [:DATAFILENAME] [:COLUMNDBNAME] [:COLUMNARFILENAME] [:NUMCOLUMNS]");
            return;
        }

        String dataFileName = args[1];
        String columnDBName = args[2];
        String columnarFileName = args[3];
        int numColumns = Integer.parseInt(args[4]);

        execute(dataFileName, columnDBName, columnarFileName, numColumns);

        return;
    }

    private static boolean isValidInput(String[] args) {
        return args.length == 5 && args[0].equals("batchinsert")
                && java.util.regex.Pattern.matches("\\d+", args[4]);
    }

    public static boolean execute(String dataFileName, String columnDBName, String columnarFileName,
            int numColumns) throws Exception {
        String[] rawRows = readFromFile(dataFileName);
        InsertedTable table = new InsertedTable(rawRows, numColumns, columnarFileName);
        new SystemDefs(columnDBName, 100, 100, null);
        return doBatchInsert(table, columnarFileName, numColumns);
    }

    private static String[] readFromFile(String dataFileName) throws Exception {
        ArrayList<String> rawRows = new ArrayList<String>();
        File fileInstance = new File(dataFileName);
        try (BufferedReader fileReader = new BufferedReader(new FileReader(fileInstance))) {
            String row;

            while ((row = fileReader.readLine()) != null) {
                rawRows.add(row);
            }

            return rawRows.toArray(new String[1]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Cannot read the file");
        }
    }

    private static boolean doBatchInsert(InsertedTable rows, String columnarFileName,
            int numColumns) throws IOException, HFException, HFBufMgrException, HFDiskMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        Columnarfile tableFile = new Columnarfile(columnarFileName, rows.header);

        for (String row : rows.rows) {
            StringTokenizer columnTokenizer = new StringTokenizer(row);
            byte[] tuple = new byte[rows.rowSizeInByte()];
            for (int i = 0, offset = 0; i < rows.numColumns; i++) {
                String cell = columnTokenizer.nextToken();
                ColumnInfo column = rows.header[i];

                if (column.type.toString() == "attrString") {
                    Convert.setStrValue(cell, offset, tuple);
                } else {
                    Convert.setIntValue(Integer.parseInt(cell), offset, tuple);
                }

                offset += column.sizeInBytes;
            }

            tableFile.insertTuple(tuple);
        }

        return true;
    }
}
