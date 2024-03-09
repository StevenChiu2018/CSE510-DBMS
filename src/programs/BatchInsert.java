package programs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import global.AttrType;
import global.Convert;
import global.SystemDefs;

// Should be put in ColumnarFile.java
class ColumnInfo {
    public String name;
    public AttrType type;
    public int sizeInByte;

    /**
     * Constructor.
     *
     * @param stringColumnInfo The format is atrname:atrtype
     */
    public ColumnInfo(String stringColumnInfo) {
        StringTokenizer columnTokenizer;

        columnTokenizer = new StringTokenizer(stringColumnInfo, ":");
        this.name = columnTokenizer.nextToken();

        columnTokenizer = new StringTokenizer(columnTokenizer.nextToken(), "(");
        if (columnTokenizer.nextToken().equals("char")) {
            this.type = new AttrType("attrString");
            columnTokenizer = new StringTokenizer(columnTokenizer.nextToken(), ")");
            this.sizeInByte = Integer.parseInt(columnTokenizer.nextToken()) * 3;
        } else {
            this.type = new AttrType("attrInteger");
            this.sizeInByte = 4;
        }
    }
}


class InsertedTable {
    public ColumnInfo[] header;
    public String[] rows;
    public int numColumns;

    public InsertedTable(String[] rawRows, int numColumns) {
        this.header = new ColumnInfo[numColumns];
        StringTokenizer columnInfoTokenizer = new StringTokenizer(rawRows[0], "\t\r");
        for (int i = 0; i < numColumns; i++) {
            this.header[i] = new ColumnInfo(columnInfoTokenizer.nextToken());
        }

        this.rows = new String[rawRows.length - 1];
        this.numColumns = numColumns;

        for (int i = 1; i < rawRows.length; i++) {
            this.rows[i - 1] = rawRows[i];
        }
    }

    public int rowSizeInByte() {
        int size = 0;
        for (ColumnInfo column : this.header) {
            size += column.sizeInByte;
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
        InsertedTable table = new InsertedTable(rawRows, numColumns);
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
            int numColumns) throws IOException {
        // ColumnarFile tableFile =
        // new ColumnarFile(columnarFileName, numColumns, rows.toColumnTypes());

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

                offset += column.sizeInByte;
            }

            // tableFile.insertTuple(tuple);
        }

        return true;
    }
}
