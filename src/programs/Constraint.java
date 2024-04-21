package programs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import global.AttrType;
import global.Convert;
import heap.Tuple;

public class Constraint {
  public Condition leftCondition;
  public String operator;
  public Condition rightCondition;

  public Constraint() {
    this.leftCondition = null;
    this.rightCondition = null;
    this.operator = "";
  }

  public Constraint(String constraintStr, ArrayList<QueryColumnInfo> columns) {
    String[] constraints = constraintStr.split(" or | and ");

    this.leftCondition = new Condition(constraints[0], columns);
    this.operator = this.getMatchSubString(constraintStr, " or | and ");
    this.rightCondition = null;

    if (this.operator.equals("or") | this.operator.equals("and")) {
      this.rightCondition = new Condition(constraints[1], columns);
    }
  }

  public static Constraint copied(Constraint constraint) {
    Constraint newConstraint = new Constraint();
    newConstraint.leftCondition = Condition.copied(constraint.leftCondition);
    newConstraint.operator = constraint.operator;
    if (constraint.rightCondition != null)
      newConstraint.rightCondition = Condition.copied(constraint.rightCondition);

    return newConstraint;
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

  public Condition() {
    this.comparedColumn = null;
    this.operator = "";
    this.comparingInt = 0;
    this.comparingString = "";
  }

  public Condition(String constraintStr, ArrayList<QueryColumnInfo> columns) {
    String[] tokens = constraintStr.split("<=|<|!=|=|>=|>");
    String columnName = tokens[0].trim();
    String value = tokens[1].trim();

    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).name.equals(columnName)) {
        this.comparedColumn = QueryColumnInfo.copied(columns.get(i));

        break;
      }
    }

    this.operator = this.getMatchSubString(constraintStr, "<=|<|!=|=|>=|>");

    if (Pattern.matches("\\d+", value)) {
      this.comparingInt = Integer.parseInt(value);
    } else {
      this.comparingString = value;
    }
  }

  public static Condition copied(Condition condition) {
    Condition newCondition = new Condition();
    newCondition.comparedColumn = QueryColumnInfo.copied(condition.comparedColumn);
    newCondition.operator = condition.operator;
    newCondition.comparingInt = condition.comparingInt;
    newCondition.comparingString = condition.comparingString;

    return newCondition;
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

      case "!=":
        return val1 != this.comparingInt;

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

      case "!=":
        return !this.comparingString.equals(val2);

      default:
        return false;
    }
  }
}
