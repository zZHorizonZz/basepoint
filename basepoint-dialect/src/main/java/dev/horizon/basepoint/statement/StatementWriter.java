package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.RenderedStatement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

  private record Scope(String qualifier, Node rows, Map<String, String> relations, Set<String> labels) {
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

  String quoteTable(String table) {
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
   * @throws IllegalStateException if no statement with a source is being rendered
   */
  public StatementWriter column(String column) {
    return append(current().qualifier() + "." + dialect.quote(column));
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
          + " Use on() only in a query that you pass to include(), includeOne(), subquery(), exists() or relation().");
    }
    Scope outer = outward.next();
    int dot = column.indexOf('.');
    if (dot < 0) {
      return append(outer.qualifier() + "." + dialect.quote(column));
    }
    String relation = column.substring(0, dot);
    if (!outer.relations().containsKey(relation)) {
      throw new IllegalStateException("The outer column " + column + " reads the relation " + relation + ", which the"
          + " enclosing query does not name. Name it with relation(\"" + relation + "\", rows) on that query.");
    }
    return append(outer.relations().get(relation) + "." + dialect.quote(column.substring(dot + 1)));
  }

  void relate(String name) {
    current().relations().put(name, alias());
  }

  void label(String name) {
    current().labels().add(name);
  }

  /**
   * Appends a column by its name: unqualified if the SELECT list of the current statement names a column so, and as
   * a column of the current source otherwise.
   *
   * @param name the column name
   * @return this writer
   */
  public StatementWriter reference(String name) {
    return current().labels().contains(name) ? name(name) : column(name);
  }

  /**
   * Returns the names of the relations of the current statement and of the statements around it.
   *
   * @return the names
   */
  public Set<String> relations() {
    Set<String> names = new HashSet<>();
    scopes.forEach(scope -> names.addAll(scope.relations().keySet()));
    return names;
  }

  /**
   * Returns the alias of a relation of the current statement, or of the nearest enclosing statement that names it.
   *
   * @param name the name of the relation
   * @return the alias, or empty if no statement here names the relation
   */
  public Optional<String> relation(String name) {
    return scopes.stream().map(scope -> scope.relations().get(name)).filter(Objects::nonNull).findFirst();
  }

  /**
   * Appends a column of a relation of the current statement: {@code alias.column}.
   *
   * @param relation the name of the relation
   * @param column the column name
   * @return this writer
   * @throws IllegalStateException if the statement has no relation of the name
   */
  public StatementWriter path(String relation, String column) {
    String alias = relation(relation).orElseThrow(() -> new IllegalStateException("The query has no relation named "
        + relation + ". Name it with relation(\"" + relation + "\", rows) on the query that reads the path."));
    return append(alias + "." + dialect.quote(column));
  }

  /**
   * Returns the qualifier of the current source.
   *
   * @return the alias or quoted table name
   * @throws IllegalStateException if no statement with a source is being rendered
   */
  public String qualifier() {
    return current().qualifier();
  }

  /**
   * Returns the current source.
   *
   * @return the table or subquery
   * @throws IllegalStateException if no statement with a source is being rendered
   */
  public Node source() {
    return current().rows();
  }

  private Scope current() {
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

  void scope(String qualifier, Node rows, Runnable body) {
    scopes.push(new Scope(qualifier, rows, new LinkedHashMap<>(), new HashSet<>()));
    try {
      body.run();
    } finally {
      scopes.pop();
    }
  }

  /**
   * Returns the SQL text and values rendered so far.
   *
   * @return the rendered statement
   */
  public RenderedStatement build() {
    return new RenderedStatement(text.toString(), params);
  }
}
