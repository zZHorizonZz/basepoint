package dev.horizon.basepoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Renders one statement tree into SQL text and placeholder values.
 *
 * <p>
 * Render every node through {@link #render(Node)}. That method passes the node to the dialect's decorator first.
 *
 * <p>
 * The writer binds a value when it writes the value's placeholder, so the values are in text order. The writer also
 * tracks the source of each statement that is being rendered. {@link #column} qualifies a column with the current
 * source, and {@link #outerColumn} qualifies it with the source of the enclosing statement.
 */
public final class StatementWriter {

  private final Dialect dialect;
  private final StringBuilder text = new StringBuilder();
  private final List<Object> params = new ArrayList<>();
  private final Deque<Scope> scopes = new ArrayDeque<>();
  private int aliases;

  public StatementWriter(Dialect dialect) {
    this.dialect = dialect;
  }

  /**
   * The source of a statement that is being rendered.
   *
   * @param qualifier the SQL that qualifies its columns
   * @param rows the table or subquery
   */
  private record Scope(String qualifier, Node rows) {
  }

  /**
   * Returns the dialect that this writer renders for.
   *
   * @return the dialect
   */
  public Dialect dialect() {
    return dialect;
  }

  /**
   * Decorates a node with the dialect's decorator and renders the result.
   *
   * @param node the node to render
   * @return this writer
   */
  public StatementWriter render(Node node) {
    dialect.decorator().decorate(node).render(this);
    return this;
  }

  /**
   * Renders nodes with a separator between them.
   *
   * @param nodes the nodes to render
   * @param separator the text between two nodes, for example {@code ", "}
   * @return this writer
   */
  public StatementWriter render(List<? extends Node> nodes, String separator) {
    for (int i = 0; i < nodes.size(); i++) {
      append(i == 0 ? "" : separator).render(nodes.get(i));
    }
    return this;
  }

  /**
   * Appends SQL text as it is.
   *
   * @param sql the text to append
   * @return this writer
   */
  public StatementWriter append(String sql) {
    text.append(sql);
    return this;
  }

  /**
   * Appends a quoted identifier.
   *
   * @param name the identifier, such as a column name or an alias
   * @return this writer
   */
  public StatementWriter name(String name) {
    return append(dialect.quote(name));
  }

  /**
   * Appends quoted identifiers, separated by commas.
   *
   * @param names the identifiers
   * @return this writer
   */
  public StatementWriter names(List<String> names) {
    for (int i = 0; i < names.size(); i++) {
      append(i == 0 ? "" : ", ").name(names.get(i));
    }
    return this;
  }

  /**
   * Appends a quoted table name.
   *
   * @param table the table name, for example {@code orders} or {@code shop.orders}
   * @return this writer
   */
  public StatementWriter table(String table) {
    return append(quoteTable(table));
  }

  /**
   * Returns a quoted table name. Each dot-separated part is quoted on its own.
   *
   * @param table the table name, for example {@code shop.orders}
   * @return the quoted name, for example {@code "shop"."orders"}
   */
  public String quoteTable(String table) {
    return String.join(".", Arrays.stream(table.split("\\.")).map(dialect::quote).toList());
  }

  /**
   * Binds a value and appends its placeholder.
   *
   * @param value the value, which can be {@code null}
   * @return this writer
   */
  public StatementWriter param(Object value) {
    append(dialect.parameters().placeholder(params.size()));
    params.add(value);
    return this;
  }

  /**
   * Appends a column of the current source.
   *
   * @param column the column name
   * @return this writer
   */
  public StatementWriter column(String column) {
    Scope scope = scopes.peek();
    return append(scope == null ? dialect.quote(column) : scope.qualifier() + "." + dialect.quote(column));
  }

  /**
   * Appends a column of the enclosing statement's source.
   *
   * @param column the column name
   * @return this writer
   * @throws IllegalStateException if the current statement has no enclosing statement
   */
  public StatementWriter outerColumn(String column) {
    Iterator<Scope> outward = scopes.iterator();
    if (outward.hasNext()) {
      outward.next();
    }
    if (!outward.hasNext()) {
      throw new IllegalStateException("The column '" + column + "' refers to an enclosing query, but there is none."
          + " Use on() only in a query that you pass to include(), includeOne(), includeCount() or exists().");
    }
    return append(outward.next().qualifier() + "." + dialect.quote(column));
  }

  /**
   * Returns the qualifier of the current source.
   *
   * @return the alias or quoted table name
   * @throws IllegalStateException if no statement with a source is being rendered
   */
  public String qualifier() {
    return scope().qualifier();
  }

  /**
   * Returns the current source.
   *
   * @return the table or subquery
   * @throws IllegalStateException if no statement with a source is being rendered
   */
  public Node source() {
    return scope().rows();
  }

  private Scope scope() {
    if (scopes.isEmpty()) {
      throw new IllegalStateException("This clause must be inside a statement that has a source.");
    }
    return scopes.peek();
  }

  /**
   * Returns a new quoted alias that is unique in this statement.
   *
   * @return the alias, for example {@code "t0"}
   */
  public String alias() {
    return dialect.quote("t" + aliases++);
  }

  /**
   * Runs a rendering step with a source as the current source.
   *
   * @param qualifier the SQL that qualifies the source's columns
   * @param rows the table or subquery
   * @param body the rendering step
   * @return this writer
   */
  public StatementWriter scoped(String qualifier, Node rows, Runnable body) {
    scopes.push(new Scope(qualifier, rows));
    try {
      body.run();
    } finally {
      scopes.pop();
    }
    return this;
  }

  /**
   * Returns the SQL text and values rendered so far.
   *
   * @return the rendered statement
   */
  public RenderedStatement rendered() {
    return new RenderedStatement(text.toString(), params);
  }
}
