package programs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import global.AttrType;
import global.Convert;
import heap.Tuple;

class Constraint {
  public Condition leftCondition;
  public String operator;
  public Condition rightCondition;

  public Constraint(String constraintStr, ArrayList<QueryColumnInfo> columns) {
    String[] constraints = constraintStr.split("or | and");

    this.leftCondition = new Condition(constraints[0], columns);
    this.operator = this.getMatchSubString(constraintStr, "or | and");
    this.rightCondition = null;

    if (this.operator.equals("or") | this.operator.equals("and")) {
      this.rightCondition = new Condition(constraints[1], columns);
    }
  }

  public boolean isSatisfying(Tuple tuple) throws IOException {
    if (this.operator.equals("and")) {
      return this.leftCondition.isSatisfying(tuple) & this.rightCondition.isSatisfying(tuple);
    } else if (this.operator.equals("or")) {
      return this.leftCondition.isSatisfying(tuple) | this.rightCondition.isSatisfying(tuple);
    } else {
      return this.leftCondition.isSatisfying(tuple);
    }
  }

  private String getMatchSubString(String source, String regex) {
    Pattern operatorPattern = Pattern.compile(regex);
    Matcher operatorMatcher = operatorPattern.matcher(source);
    if (operatorMatcher.find()) {
      return operatorMatcher.group(0).replaceAll("\\s", "");
    } else {
      return "";
    }
  }
}


class Condition {
  public QueryColumnInfo comparedColumn;
  public String operator;
  public int comparingInt;
  public String comparingString;

  public Condition(String constraintStr, ArrayList<QueryColumnInfo> columns) {
    String[] tokens = constraintStr.split("< | <= | = | >= | >");
    String columnName = tokens[0].replaceAll("\\s", "");
    String value = tokens[1].replaceAll("\\s", "");

    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).name.equals(columnName)) {
        this.comparedColumn = columns.get(i);

        break;
      }
    }

    this.operator = this.getMatchSubString(constraintStr, "< | <= | = | >= | >");

    if (Pattern.matches("\\d+", value)) {
      this.comparingInt = Integer.parseInt(value);
    } else {
      this.comparingString = value;
    }
  }

  private String getMatchSubString(String source, String regex) {
    Pattern operatorPattern = Pattern.compile(regex);
    Matcher operatorMatcher = operatorPattern.matcher(source);
    operatorMatcher.find();

    return operatorMatcher.group(0).replaceAll("\\s", "");
  }

  public boolean isSatisfying(Tuple tuple) throws IOException {
    if (this.comparedColumn.columnInfo.type.attrType == AttrType.attrInteger) {
      return this.doCompareInteger(tuple);
    } else {
      return this.doCompareString(tuple);
    }
  }

  private boolean doCompareInteger(Tuple tuple) throws IOException {
    int val1 = Convert.getIntValue(this.comparedColumn.tupleOffset, tuple.getTupleByteArray());

    switch (operator) {
      case ">":
        return val1 > this.comparingInt;

      case "<":
        return val1 < this.comparingInt;

      case "=":
        return val1 == this.comparingInt;

      case ">=":
        return val1 >= this.comparingInt;

      case "<=":
        return val1 <= this.comparingInt;

      default:
        return false;
    }
  }

  private boolean doCompareString(Tuple tuple) throws IOException {
    String val2 = Convert.getStrValue(this.comparedColumn.tupleOffset, tuple.getTupleByteArray(),
        this.comparedColumn.columnInfo.sizeInBytes);

    switch (this.operator) {
      case "=":
        return this.comparingString.equals(val2);

      default:
        return false;
    }
  }
}
