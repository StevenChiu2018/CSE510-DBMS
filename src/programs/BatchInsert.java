package programs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import global.SystemDefs;

public class BatchInsert {
    public static void main(String[] args) throws Exception {
        String command = args[0];

        if (!command.equals("batchinsert")) {
            System.out.println(
                    "batchinsert [:DATAFILENAME] [:COLUMNDBNAME] [:COLUMNARFILENAME] [:NUMCOLUMNS]");
            return;
        }

        String dataFileName = args[1];
        String columnDBName = args[2];
        String columnarFileName = args[3];
        String numColumns = args[4];

        execute(dataFileName, columnDBName, columnarFileName, numColumns);

        return;
    }

    public static boolean execute(String dataFileName, String columnDBName, String columnarFileName,
            String numColumns) throws Exception {
        String[] rawRows = readFromFile(dataFileName);
        SystemDefs server = new SystemDefs(columnDBName, 10000000, 100, null);
        // return doBatchInsert(rawRows, columnarFileName, numColumns);
        return true;
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
}
