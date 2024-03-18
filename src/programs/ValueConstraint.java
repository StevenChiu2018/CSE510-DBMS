package programs;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class ValueConstraint {
  public String columnName;
  public String operator;
  public String stringValue;
  public int intValue;
  public int type; // 0: int, 1: string

  public ValueConstraint(String constraintString) {
    StringTokenizer tokenServer = new StringTokenizer(constraintString);

    this.columnName = tokenServer.nextToken();
    this.operator = tokenServer.nextToken();

    String boundValue = tokenServer.nextToken();
    if (Pattern.matches("\\d+", boundValue)) {
      stringValue = "";
      intValue = Integer.parseInt(boundValue);
      type = 0;
    } else {
      stringValue = boundValue;
      intValue = 0;
      type = 1;
    }
  }
}
