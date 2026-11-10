package dev.horizon.basepoint;

import dev.horizon.basepoint.impl.QueryImpl;
import java.math.BigDecimal;
import java.time.Duration;

import java.util.List;
import java.util.Objects;

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
   * Returns a query that gives one value, used as a value: {@code (SELECT ...)}. Use {@link Query#on} to relate it to
   * the current row, for example {@code subquery(items.query().on("order_id", "id").count())} for the number of
   * items of each order.
   *
   * @param query the query, from {@link CollectionReference#query()}. It selects one column and gives at most one
   * row, for example with {@link Query#count()}, an aggregate in {@link Query#select}, or {@link Query#limit}.
   * @return the value
   * @throws IllegalArgumentException if the query selects all columns or more than one
   */
  static Expression subquery(Query query) {
    return new Subquery(QueryImpl.related(query).value());
  }

  /**
   * Returns the sum of two numbers: {@code (left + right)}.
   *
   * @param left the first number
   * @param right the second number
   * @return the expression
   */
  static Expression add(Expression left, Expression right) {
    return new Arithmetic(left, Arithmetic.Operator.ADD, right);
  }

  /**
   * Returns the difference of two numbers: {@code (left - right)}.
   *
   * @param left the number
   * @param right the number to subtract
   * @return the expression
   */
  static Expression subtract(Expression left, Expression right) {
    return new Arithmetic(left, Arithmetic.Operator.SUBTRACT, right);
  }

  /**
   * Returns the product of two numbers: {@code (left * right)}.
   *
   * @param left the first number
   * @param right the second number
   * @return the expression
   */
  static Expression multiply(Expression left, Expression right) {
    return new Arithmetic(left, Arithmetic.Operator.MULTIPLY, right);
  }

  /**
   * Returns the quotient of two numbers: {@code (left / right)}. With two integers, PostgreSQL, SQL Server and SQLite
   * give an integer, and MariaDB gives a decimal.
   *
   * @param left the number
   * @param right the divisor
   * @return the expression
   */
  static Expression divide(Expression left, Expression right) {
    return new Arithmetic(left, Arithmetic.Operator.DIVIDE, right);
  }

  /**
   * Returns a timestamp moved later by a duration, for example {@code add(now(), Duration.ofHours(2))}. Each database
   * moves it to the microsecond, and SQLite to the millisecond.
   *
   * @param timestamp the timestamp
   * @param duration the duration
   * @return the expression
   */
  static Expression add(Expression timestamp, Duration duration) {
    return new Shift(timestamp, duration);
  }

  /**
   * Returns a timestamp moved earlier by a duration, for example {@code subtract(now(), Duration.ofDays(7))} for the
   * time a week ago. Each database moves it to the microsecond, and SQLite to the millisecond.
   *
   * @param timestamp the timestamp
   * @param duration the duration
   * @return the expression
   */
  static Expression subtract(Expression timestamp, Duration duration) {
    return new Shift(timestamp, duration.negated());
  }

  /**
   * Returns the texts joined into one, for example {@code concat(column("first_name"), value(" "),
   * column("last_name"))}. The result is {@code NULL} if one of the texts is {@code NULL}, on every database.
   *
   * @param texts the texts, at least two
   * @return the expression
   * @throws IllegalArgumentException if there are fewer than two texts
   */
  static Expression concat(Expression... texts) {
    return new Concat(List.of(texts));
  }

  /**
   * Returns a text in lower case: {@code LOWER(text)}.
   *
   * @param text the text
   * @return the expression
   */
  static Expression lower(Expression text) {
    return new Call("LOWER", List.of(text));
  }

  /**
   * Returns a text in upper case: {@code UPPER(text)}.
   *
   * @param text the text
   * @return the expression
   */
  static Expression upper(Expression text) {
    return new Call("UPPER", List.of(text));
  }

  /**
   * Returns a text without the spaces at its start and its end: {@code TRIM(text)}.
   *
   * @param text the text
   * @return the expression
   */
  static Expression trim(Expression text) {
    return new Call("TRIM", List.of(text));
  }

  /**
   * Returns the number of characters of a text, spaces at its end included, on every database.
   *
   * @param text the text
   * @return the expression
   */
  static Expression length(Expression text) {
    return new Length(text);
  }

  /**
   * Returns the absolute value of a number: {@code ABS(number)}.
   *
   * @param number the number
   * @return the expression
   */
  static Expression abs(Expression number) {
    return new Call("ABS", List.of(number));
  }

  /**
   * Returns the first value that is not {@code NULL}: {@code COALESCE(values)}.
   *
   * @param values the values, at least two
   * @return the expression
   * @throws IllegalArgumentException if there are fewer than two values
   */
  static Expression coalesce(Expression... values) {
    if (values.length < 2) {
      throw new IllegalArgumentException("coalesce needs at least two values, but has " + values.length + ".");
    }
    return new Call("COALESCE", List.of(values));
  }

  /**
   * Returns the current time in UTC, in the form that each database stores a timestamp.
   *
   * @return the expression
   */
  static Expression now() {
    return new Now();
  }

  /**
   * Returns one of two values, by a condition: {@code CASE WHEN condition THEN then ELSE otherwise END}.
   *
   * @param condition the condition
   * @param then the value when the condition is true
   * @param otherwise the value when the condition is false or {@code NULL}
   * @return the expression
   */
  static Expression when(Filter condition, Expression then, Expression otherwise) {
    return new Case(condition, then, otherwise);
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
   * An arithmetic operation of two numbers, such as {@code (left + right)}.
   *
   * @param left the first number
   * @param operator the operator
   * @param right the second number
   */
  record Arithmetic(Expression left, Operator operator, Expression right) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(left).append(" " + operator.symbol() + " ").render(right).append(")");
    }

    /** An arithmetic operator. */
    public enum Operator {
      /** {@code +}. */
      ADD("+"),
      /** {@code -}. */
      SUBTRACT("-"),
      /** {@code *}. */
      MULTIPLY("*"),
      /** {@code /}. */
      DIVIDE("/");

      private final String symbol;

      Operator(String symbol) {
        this.symbol = symbol;
      }

      /**
       * Returns the SQL of the operator.
       *
       * @return the symbol, for example {@code +}
       */
      public String symbol() {
        return symbol;
      }
    }
  }

  /**
   * Texts joined into one, rendered as the standard {@code (a || b)}. Dialects whose database joins texts otherwise
   * decorate it.
   *
   * @param texts the texts, at least two
   */
  record Concat(List<Expression> texts) implements Expression {

    public Concat {
      texts = List.copyOf(texts);
      if (texts.size() < 2) {
        throw new IllegalArgumentException("concat needs at least two texts, but has " + texts.size() + ".");
      }
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(texts, " || ").append(")");
    }
  }

  /**
   * A function that every database writes the same way, such as {@code LOWER(text)}.
   *
   * @param name the function name, for example {@code LOWER}
   * @param arguments the arguments
   */
  record Call(String name, List<Expression> arguments) implements Expression {

    public Call {
      arguments = List.copyOf(arguments);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append(name + "(").render(arguments, ", ").append(")");
    }
  }

  /**
   * The number of characters of a text, rendered as the standard {@code CHAR_LENGTH(text)}. Dialects whose database
   * counts otherwise decorate it.
   *
   * @param text the text
   */
  record Length(Expression text) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("CHAR_LENGTH(").render(text).append(")");
    }
  }

  /**
   * The current time in UTC, rendered as the standard {@code CURRENT_TIMESTAMP}. Dialects whose database gives
   * another time or form decorate it.
   */
  record Now() implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("CURRENT_TIMESTAMP");
    }
  }

  /**
   * A timestamp moved by a duration, rendered as {@code (timestamp + INTERVAL 'n seconds')}, with the seconds to the
   * microsecond. Dialects whose database moves a timestamp otherwise decorate it.
   *
   * @param timestamp the timestamp
   * @param duration the duration, earlier when it is negative
   */
  record Shift(Expression timestamp, Duration duration) implements Expression {

    public Shift {
      Objects.requireNonNull(timestamp, "timestamp");
      Objects.requireNonNull(duration, "duration");
    }

    /**
     * Returns the duration in whole microseconds.
     *
     * @return the microseconds, negative for an earlier time
     */
    public long microseconds() {
      return Math.addExact(Math.multiplyExact(duration.getSeconds(), 1_000_000L), duration.getNano() / 1_000);
    }

    /**
     * Returns the duration in seconds, to the microsecond.
     *
     * @return the seconds, for example {@code -86400.000000}, negative for an earlier time
     */
    public BigDecimal seconds() {
      return BigDecimal.valueOf(microseconds(), 6);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(timestamp).append(" + INTERVAL '" + seconds().toPlainString() + " seconds')");
    }
  }

  /**
   * One of two values, by a condition: {@code CASE WHEN condition THEN then ELSE otherwise END}.
   *
   * @param condition the condition
   * @param then the value when the condition is true
   * @param otherwise the value when the condition is false or {@code NULL}
   */
  record Case(Filter condition, Expression then, Expression otherwise) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("CASE WHEN ").render(condition).append(" THEN ").render(then).append(" ELSE ").render(otherwise)
          .append(" END");
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
