package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.InvalidQueryException;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.statement.Node;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
   * Returns the name of the column that the expression gives in a SELECT list.
   *
   * @return the name, or empty if the database names the column
   */
  default Optional<String> label() {
    return Optional.empty();
  }

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
   * Returns a bound value. If {@code value} is already an expression, it is returned unchanged, and {@code null} is
   * SQL {@code NULL}.
   *
   * @param value the value, an expression, or {@code null}
   * @return the expression
   */
  static Expression value(Object value) {
    return switch (value) {
      case null -> new Null();
      case Expression expression -> expression;
      default -> new Value(value);
    };
  }

  /**
   * Returns a column of a relation of the query, a path: {@code column("customer", "region")} reads the column
   * {@code region} of the row that the relation {@code customer} names.
   *
   * @param relation the name of the relation, from {@link Query#relation}
   * @param name the column name
   * @return the expression
   */
  static Expression column(String relation, String name) {
    return new Path(relation, name);
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
    return new CountAll();
  }

  /**
   * Returns {@code COUNT(column)}, the number of rows where the column is not {@code NULL}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression count(String column) {
    return new Aggregate(Aggregate.Function.COUNT, column(column));
  }

  /**
   * Returns {@code SUM(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression sum(String column) {
    return new Aggregate(Aggregate.Function.SUM, column(column));
  }

  /**
   * Returns {@code AVG(column)}. The result type depends on the database.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression avg(String column) {
    return new Aggregate(Aggregate.Function.AVG, column(column));
  }

  /**
   * Returns {@code MIN(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression min(String column) {
    return new Aggregate(Aggregate.Function.MIN, column(column));
  }

  /**
   * Returns {@code MAX(column)}.
   *
   * @param column the column name
   * @return the expression
   */
  static Expression max(String column) {
    return new Aggregate(Aggregate.Function.MAX, column(column));
  }

  /**
   * Returns a query that gives one value, used as a value: {@code (SELECT ...)}. Use {@link Query#on} to relate it to
   * the current row, for example {@code subquery(items.query().on("order_id", "id").count())} for the number of
   * items of each order.
   *
   * @param query the query, from {@link CollectionReference#query()}. It selects one column and gives at most one
   * row, for example with {@link Query#count()}, an aggregate in {@link Query#select}, or {@link Query#limit}.
   * @return the value
   * @throws InvalidQueryException if the query selects all columns or more than one, or has a WITH query
   */
  static Expression subquery(Query query) {
    Statement statement = query.statement();
    Select select = statement.find(Select.class).orElseThrow();
    if (select.allColumns() || select.columns().size() != 1) {
      throw new InvalidQueryException("A subquery selects one column, but this query selects " + (select.allColumns()
          ? "all columns"
          : select.columns().size() + " columns") + ". Select one with count(), select(name, value) or columns(name).");
    }
    return new Subquery(statement);
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
    return new Call(Call.Function.LOWER, List.of(text));
  }

  /**
   * Returns a text in upper case: {@code UPPER(text)}.
   *
   * @param text the text
   * @return the expression
   */
  static Expression upper(Expression text) {
    return new Call(Call.Function.UPPER, List.of(text));
  }

  /**
   * Returns a text without the spaces at its start and its end: {@code TRIM(text)}.
   *
   * @param text the text
   * @return the expression
   */
  static Expression trim(Expression text) {
    return new Call(Call.Function.TRIM, List.of(text));
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
    return new Call(Call.Function.ABS, List.of(number));
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
    return new Call(Call.Function.COALESCE, List.of(values));
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
}
