package dev.horizon.basepoint;

/**
 * A value in a statement: a column, a bound value or an aggregate. A filter compares expressions, a SELECT gives them
 * as its columns, and an UPDATE sets them.
 *
 * <p>
 * An expression writes itself, thus a library or an application adds its own by implementing this interface, and it
 * composes with the expressions and filters of basepoint.
 */
public interface Expression {

  /**
   * Writes the expression.
   *
   * @param sql the writer of the statement
   */
  void write(StatementWriter sql);

  /**
   * Gives a column of the table of the statement or subquery.
   *
   * @param name the name of the column
   * @return the expression
   */
  static Expression column(String name) {
    return new Column(name);
  }

  /**
   * Gives a value, which is bound.
   *
   * @param value the value, or null
   * @return the expression
   */
  static Expression value(Object value) {
    return value instanceof Expression expression ? expression : new Value(value);
  }

  /**
   * Gives the number of rows: {@code COUNT(*)}.
   *
   * @return the expression
   */
  static Expression count() {
    return new Aggregate("COUNT", null);
  }

  /**
   * Gives the number of rows in which a column is not null.
   *
   * @param column the name of the column
   * @return the expression
   */
  static Expression count(String column) {
    return new Aggregate("COUNT", column(column));
  }

  /**
   * Gives the sum of a column.
   *
   * @param column the name of the column
   * @return the expression
   */
  static Expression sum(String column) {
    return new Aggregate("SUM", column(column));
  }

  /**
   * Gives the average of a column, in the type that the database gives it.
   *
   * @param column the name of the column
   * @return the expression
   */
  static Expression avg(String column) {
    return new Aggregate("AVG", column(column));
  }

  /**
   * Gives the least value of a column.
   *
   * @param column the name of the column
   * @return the expression
   */
  static Expression min(String column) {
    return new Aggregate("MIN", column(column));
  }

  /**
   * Gives the greatest value of a column.
   *
   * @param column the name of the column
   * @return the expression
   */
  static Expression max(String column) {
    return new Aggregate("MAX", column(column));
  }

  /**
   * A column of the table of the statement or subquery.
   *
   * @param name the name of the column
   */
  record Column(String name) implements Expression {

    @Override
    public void write(StatementWriter sql) {
      sql.column(name);
    }
  }

  /**
   * A value, which is bound.
   *
   * @param value the value, or null
   */
  record Value(Object value) implements Expression {

    @Override
    public void write(StatementWriter sql) {
      sql.param(value);
    }
  }

  /**
   * An aggregate function of the rows of a group.
   *
   * @param function the name of the function, such as {@code SUM}
   * @param argument the value that it aggregates, or null for {@code *}
   */
  record Aggregate(String function, Expression argument) implements Expression {

    @Override
    public void write(StatementWriter sql) {
      sql.append(function).append("(");
      if (argument == null) {
        sql.append("*");
      } else {
        sql.write(argument);
      }
      sql.append(")");
    }
  }
}
