package dev.horizon.basepoint;

import java.util.List;

/**
 * A value in a statement, such as a column, a bound value, an aggregate or a subquery.
 *
 * <p>
 * Filters compare expressions, SELECT lists contain expressions, and updates assign expressions. To add your own
 * expression, implement this interface and render it with {@link StatementWriter}.
 */
@FunctionalInterface
public interface Expression extends Node {

  /**
   * Returns a column of the current source.
   *
   * @param name the column name
   * @return the expression
   */
  static Expression column(String name) {
    return new Column(name);
  }

  /**
   * Returns a bound value. If {@code value} is already an expression, it is returned unchanged.
   *
   * @param value the value, an expression, or {@code null}
   * @return the expression
   */
  static Expression value(Object value) {
    return value instanceof Expression expression ? expression : new Value(value);
  }

  /**
   * Returns the value of a key of the JSON object in a column, or {@code NULL} if the object has no such key. Compare
   * it with {@link Filter#eq(Expression, Object)}, and match the rows whose object has the key with
   * {@code ne(key(column, name), null)}.
   *
   * <p>
   * The value is text: a number in the object, such as {@code 5}, is the text {@code "5"}. Each dialect renders it
   * with the JSON functions of its database.
   *
   * @param column the column name, of a JSON column
   * @param key the key, which is not empty and contains neither {@code "} nor {@code \}, because the databases
   * escape those differently in a path
   * @return the expression
   * @throws IllegalArgumentException if the key is empty or contains {@code "} or {@code \}
   */
  static Expression key(String column, String key) {
    return new JsonKey(column, key);
  }

  /**
   * Returns {@code COUNT(*)}.
   *
   * @return the expression
   */
  static Expression count() {
    return new Aggregate("COUNT", null);
  }

  /**
   * Returns {@code COUNT(column)}, the number of rows where the column is not {@code NULL}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression count(String column) {
    return new Aggregate("COUNT", column(column));
  }

  /**
   * Returns {@code SUM(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression sum(String column) {
    return new Aggregate("SUM", column(column));
  }

  /**
   * Returns {@code AVG(column)}. The result type depends on the database.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression avg(String column) {
    return new Aggregate("AVG", column(column));
  }

  /**
   * Returns {@code MIN(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression min(String column) {
    return new Aggregate("MIN", column(column));
  }

  /**
   * Returns {@code MAX(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression max(String column) {
    return new Aggregate("MAX", column(column));
  }

  /**
   * A column of the current source, qualified with the source's alias or table name.
   *
   * @param name the column name
   */
  record Column(String name) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.column(name);
    }
  }

  /**
   * The value of a key of the JSON object in a column. It renders as the standard {@code JSON_VALUE(column, ?)}, with
   * the path of the key bound. Dialects whose database has no {@code JSON_VALUE} decorate it.
   *
   * @param column the column name
   * @param key the key
   */
  record JsonKey(String column, String key) implements Expression {

    public JsonKey {
      if (key.isEmpty() || key.contains("\"") || key.contains("\\")) {
        throw new IllegalArgumentException("A JSON key must not be empty or contain \" or \\, but is " + key + ".");
      }
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("JSON_VALUE(").column(column).append(", ").param(path()).append(")");
    }

    /**
     * Returns the path of the key, for example {@code $."env"}.
     *
     * @return the path
     */
    public String path() {
      return "$.\"" + key + "\"";
    }
  }

  /** All columns of the current source, for example {@code "t0".*}. */
  record AllColumns() implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append(sql.qualifier() + ".*");
    }
  }

  /**
   * An unqualified name, such as the alias of an {@link As} column.
   *
   * @param name the name
   */
  record Name(String name) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.name(name);
    }
  }

  /**
   * A bound value, rendered as a placeholder.
   *
   * @param value the value, which can be {@code null}
   */
  record Value(Object value) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.param(value);
    }
  }

  /**
   * An integer literal, rendered in the SQL text.
   *
   * @param value the integer
   */
  record Literal(long value) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append(Long.toString(value));
    }
  }

  /** The {@code NULL} literal. */
  record Null() implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("NULL");
    }
  }

  /**
   * The sum of two values: {@code (left + right)}.
   *
   * @param left the first value
   * @param right the second value
   */
  record Plus(Expression left, Expression right) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(left).append(" + ").render(right).append(")");
    }
  }

  /**
   * An aggregate function, such as {@code SUM(total)}.
   *
   * @param function the function name, for example {@code SUM}
   * @param argument the aggregated value, or {@code null} to render {@code *}
   */
  record Aggregate(String function, Expression argument) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append(function + "(");
      if (argument == null) {
        sql.append("*");
      } else {
        sql.render(argument);
      }
      sql.append(")");
    }
  }

  /**
   * {@code ROW_NUMBER() OVER (ORDER BY ...)}.
   *
   * @param sorts the sort keys of the window
   */
  record RowNumber(List<Clause.Sort> sorts) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("ROW_NUMBER() OVER (ORDER BY ").render(sorts, ", ").append(")");
    }
  }

  /**
   * A value with an alias: {@code value AS name}.
   *
   * @param value the value
   * @param name the alias
   */
  record As(Expression value, String name) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.render(value).append(" AS ").name(name);
    }
  }

  /**
   * A statement in parentheses, used as a value or as a source.
   *
   * @param statement the statement
   */
  record Subquery(Statement statement) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(statement).append(")");
    }
  }
}
