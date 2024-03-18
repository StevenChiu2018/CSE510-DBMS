package diskmgr;

public class Pcounter {
  public static int rcounter;
  public static int wcounter;

  public static void initialize() {
    rcounter = 0;
    wcounter = 0;
  }

  public static void readIncrement() {
    rcounter++;
  }

  public static void writeIncrement() {
    wcounter++;
  }

  public static String usage_in_string() {
    return "\nPage usage: " + rcounter + " page(s) read, " + wcounter + " page(s) write.\n";
  }
}
