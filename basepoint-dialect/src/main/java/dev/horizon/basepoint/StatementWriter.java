package dev.horizon.basepoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Writes one statement in the SQL of a dialect: its text, and its values as placeholders.
 *
 * <p>
 * A value is bound when its placeholder is written, thus the values are bound in the order of the text. The writer
 * keeps the tables that a column names: each SELECT, UPDATE and DELETE writes its columns qualified with its own
 * table, and a subquery names the columns of the table around it with {@link #outerColumn}.
 */
public final class StatementWriter {

  private final Dialect dialect;
  private final StringBuilder text = new StringBuilder();
  private final List<Object> params = new ArrayList<>();
  private final Deque<String> qualifiers = new ArrayDeque<>();
  private int aliases;

  public StatementWriter(Dialect dialect) {
    this.dialect = dialect;
  }

  /**
   * Gives the dialect that the statement is written in.
   *
   * @return the dialect
   */
  public Dialect dialect() {
    return dialect;
  }

  /**
   * Writes SQL that binds nothing.
   *
   * @param sql the SQL
   * @return this writer
   */
  public StatementWriter append(String sql) {
    text.append(sql);
    return this;
  }

  /**
   * Writes a name, in the quotes of the dialect.
   *
   * @param name the name of a column, an alias or a table without its schema
   * @return this writer
   */
  public StatementWriter name(String name) {
    return append(dialect.quote(name));
  }

  /**
   * Writes the name of a table, whose parts, such as its schema, are separated by dots.
   *
   * @param table the name, for example {@code shop.orders}
   * @return this writer
   */
  public StatementWriter table(String table) {
    return append(quoteTable(table));
  }

  /**
   * Gives the name of a table, whose parts are separated by dots, in the quotes of the dialect.
   *
   * @param table the name
   * @return the quoted name
   */
  public String quoteTable(String table) {
    return String.join(".", Arrays.stream(table.split("\\.")).map(dialect::quote).toList());
  }

  /**
   * Binds a value and writes its placeholder.
   *
   * @param value the value, or null
   * @return this writer
   */
  public StatementWriter param(Object value) {
    append(dialect.parameters().placeholder(params.size()));
    params.add(value);
    return this;
  }

  /**
   * Writes a column of the table of the statement or subquery that is being written.
   *
   * @param column the name of the column
   * @return this writer
   */
  public StatementWriter column(String column) {
    String qualifier = qualifiers.peek();
    return append(qualifier == null ? dialect.quote(column) : qualifier + "." + dialect.quote(column));
  }

  /**
   * Writes a column of the table around the subquery that is being written.
   *
   * @param column the name of the column
   * @return this writer
   * @throws IllegalStateException if no table is around the subquery
   */
  public StatementWriter outerColumn(String column) {
    Iterator<String> outward = qualifiers.iterator();
    outward.next();
    if (!outward.hasNext()) {
      throw new IllegalStateException("on(...) relates the rows to the rows around them, but '" + column
          + "' has no rows around it: use it in include or exists");
    }
    return append(outward.next() + "." + dialect.quote(column));
  }

  /**
   * Writes an expression.
   *
   * @param expression the expression
   * @return this writer
   */
  public StatementWriter write(Expression expression) {
    expression.write(this);
    return this;
  }

  /**
   * Writes a filter.
   *
   * @param filter the filter
   * @return this writer
   */
  public StatementWriter write(Filter filter) {
    filter.write(this);
    return this;
  }

  /**
   * Gives a new alias of a table or a subquery, quoted, which no other part of the statement has.
   *
   * @return the alias
   */
  public String alias() {
    return dialect.quote("t" + aliases++);
  }

  /**
   * Writes a part of the statement whose columns are of a table.
   *
   * @param qualifier the SQL that names the table, such as its alias
   * @param body writes the part
   * @return this writer
   */
  public StatementWriter scoped(String qualifier, Runnable body) {
    qualifiers.push(qualifier);
    try {
      body.run();
    } finally {
      qualifiers.pop();
    }
    return this;
  }

  /**
   * Gives the statement.
   *
   * @return the text and its values
   */
  public Statement toStatement() {
    return new Statement(text.toString(), params);
  }
}
