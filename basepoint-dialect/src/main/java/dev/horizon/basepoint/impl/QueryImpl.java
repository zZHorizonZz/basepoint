package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Combinable;
import dev.horizon.basepoint.CompoundQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;

import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A read of the rows of a table. Each step gives a new query.
 *
 * <p>
 * The query writes itself as a statement, as a table of a compound query, as the related rows of an include and as
 * the rows of EXISTS. Each SELECT names its table with an alias of its own, so that the related rows of a table to
 * itself and the rows around them never mix.
 */
public final class QueryImpl implements Query, JsonSyntax.Rows {

  private final Dialect dialect;
  private final String table;
  private final Steps steps;

  private QueryImpl(Dialect dialect, String table, Steps steps) {
    this.dialect = dialect;
    this.table = table;
    this.steps = steps;
  }

  public QueryImpl(Dialect dialect, String table) {
    this(dialect, table, new Steps(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(), false, null, null));
  }

  /**
   * What the steps of a query gave.
   *
   * @param columns the columns that it names
   * @param values the values that it gives as columns
   * @param includes the related rows that it gives as columns
   * @param on the pairs of columns that relate the rows to the row around them
   * @param filters the filters of WHERE
   * @param groupBy the columns of the groups
   * @param having the filters of the groups
   * @param order the order
   * @param distinct true to give each row once
   * @param limit the largest number of rows, or null for all
   * @param offset the number of rows to skip, or null for none
   */
  private record Steps(List<String> columns, List<Value> values, List<JsonSyntax.Include> includes, List<On> on,
      List<Filter> filters, List<String> groupBy, List<Filter> having, List<Sort> order, boolean distinct, Long limit,
      Long offset) {

    Steps withColumns(List<String> more) {
      return new Steps(append(columns, more), values, includes, on, filters, groupBy, having, order, distinct, limit,
          offset);
    }

    Steps withValue(Value value) {
      return new Steps(columns, append(values, List.of(value)), includes, on, filters, groupBy, having, order,
          distinct, limit, offset);
    }

    Steps withInclude(JsonSyntax.Include include) {
      return new Steps(columns, values, append(includes, List.of(include)), on, filters, groupBy, having, order,
          distinct, limit, offset);
    }

    Steps withOn(On pair) {
      return new Steps(columns, values, includes, append(on, List.of(pair)), filters, groupBy, having, order,
          distinct, limit, offset);
    }

    Steps withFilter(Filter filter) {
      return new Steps(columns, values, includes, on, append(filters, List.of(filter)), groupBy, having, order,
          distinct, limit, offset);
    }

    Steps withGroupBy(List<String> more) {
      return new Steps(columns, values, includes, on, filters, append(groupBy, more), having, order, distinct, limit,
          offset);
    }

    Steps withHaving(Filter filter) {
      return new Steps(columns, values, includes, on, filters, groupBy, append(having, List.of(filter)), order,
          distinct, limit, offset);
    }

    Steps withSort(Sort sort) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, append(order, List.of(sort)),
          distinct, limit, offset);
    }

    Steps withDistinct() {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, true, limit, offset);
    }

    Steps withLimit(Long rows) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, rows, offset);
    }

    Steps withOffset(Long rows) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, limit, rows);
    }

    boolean paged() {
      return limit != null || offset != null;
    }

    private static <T> List<T> append(List<T> list, List<T> items) {
      List<T> more = new ArrayList<>(list);
      more.addAll(items);
      return List.copyOf(more);
    }
  }

  /**
   * A value that the query gives as a column.
   *
   * @param name the name of the column
   * @param value the value
   */
  private record Value(String name, Expression value) {
  }

  /**
   * A pair of columns that relates the rows to the row around them.
   *
   * @param column the column of these rows
   * @param outerColumn the column of the row around them
   */
  private record On(String column, String outerColumn) {
  }

  /**
   * An order of the rows: by a name, of a column or of a column that the query gives, or by a value.
   *
   * @param name the name, or null
   * @param value the value, or null
   * @param direction the direction
   */
  private record Sort(String name, Expression value, Direction direction) {
  }

  private QueryImpl with(Steps next) {
    return new QueryImpl(dialect, table, next);
  }

  @Override
  public Query where(Filter filter) {
    return with(steps.withFilter(filter));
  }

  @Override
  public Query where(String filter) {
    return where(Filter.of(filter));
  }

  @Override
  public Query columns(String... columns) {
    return with(steps.withColumns(List.of(columns)));
  }

  @Override
  public Query select(String column, Expression value) {
    return with(steps.withValue(new Value(column, value)));
  }

  @Override
  public Query count() {
    return select("count", Expression.count());
  }

  @Override
  public Query include(String column, Query rows) {
    return with(steps.withInclude(new JsonSyntax.Include(column, JsonSyntax.Shape.MANY, related(rows))));
  }

  @Override
  public Query includeOne(String column, Query row) {
    return with(steps.withInclude(new JsonSyntax.Include(column, JsonSyntax.Shape.ONE, related(row.limit(1)))));
  }

  @Override
  public Query includeCount(String column, Query rows) {
    return with(steps.withInclude(new JsonSyntax.Include(column, JsonSyntax.Shape.COUNT, related(rows))));
  }

  @Override
  public Query on(String column, String outerColumn) {
    return with(steps.withOn(new On(column, outerColumn)));
  }

  @Override
  public Query groupBy(String... columns) {
    return with(steps.withGroupBy(List.of(columns)));
  }

  @Override
  public Query having(Filter filter) {
    return with(steps.withHaving(filter));
  }

  @Override
  public Query orderBy(String column) {
    return orderBy(column, Direction.ASC);
  }

  @Override
  public Query orderBy(String column, Direction direction) {
    return with(steps.withSort(new Sort(column, null, direction)));
  }

  @Override
  public Query orderBy(Expression value, Direction direction) {
    return with(steps.withSort(new Sort(null, value, direction)));
  }

  @Override
  public Query limit(long rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("the limit must be positive, but is " + rows);
    }
    return with(steps.withLimit(rows));
  }

  @Override
  public Query offset(long rows) {
    if (rows < 0) {
      throw new IllegalArgumentException("the offset must not be negative, but is " + rows);
    }
    return with(steps.withOffset(rows));
  }

  @Override
  public Query distinct() {
    return with(steps.withDistinct());
  }

  @Override
  public CompoundQuery union(Combinable other) {
    return new CompoundQueryImpl(this, CompoundQueryImpl.Operator.UNION_ALL, other);
  }

  @Override
  public CompoundQuery intersect(Combinable other) {
    return new CompoundQueryImpl(this, CompoundQueryImpl.Operator.INTERSECT, other);
  }

  @Override
  public CompoundQuery except(Combinable other) {
    return new CompoundQueryImpl(this, CompoundQueryImpl.Operator.EXCEPT, other);
  }

  @Override
  public Statement build() {
    StatementWriter sql = new StatementWriter(dialect);
    writeStatement(sql);
    return sql.toStatement();
  }

  @Override
  public boolean allColumns() {
    return steps.columns().isEmpty() && steps.values().isEmpty();
  }

  @Override
  public List<JsonSyntax.Output> outputs() {
    List<JsonSyntax.Output> outputs = new ArrayList<>();
    steps.columns().forEach(name -> outputs.add(new JsonSyntax.Output(name, false)));
    steps.values().forEach(value -> outputs.add(new JsonSyntax.Output(value.name(), false)));
    steps.includes().forEach(include -> outputs.add(new JsonSyntax.Output(include.name(),
        include.shape() != JsonSyntax.Shape.COUNT)));
    return outputs;
  }

  @Override
  public boolean ordered() {
    return !steps.order().isEmpty();
  }

  @Override
  public void write(StatementWriter sql, boolean numbered, boolean json) {
    write(sql, true, numbered, json);
  }

  @Override
  public void writeCount(StatementWriter sql) {
    String alias = sql.alias();
    sql.scoped(alias, () -> {
      sql.append("SELECT COUNT(*) FROM ").table(table).append(" AS ").append(alias);
      where(sql);
    });
  }

  /** Gives the dialect of the collection of the query. */
  public Dialect dialect() {
    return dialect;
  }

  /** Writes the query as a statement, with the order of its rows. */
  public void writeStatement(StatementWriter sql) {
    write(sql, true, false, false);
  }

  /** Writes {@code SELECT 1} of the rows, for EXISTS. */
  public void writeExists(StatementWriter sql) {
    String alias = sql.alias();
    sql.scoped(alias, () -> {
      sql.append("SELECT 1 FROM ").table(table).append(" AS ").append(alias);
      where(sql);
    });
  }

  /** Writes the query as a table of a compound query, whose rows have no order unless it limits them. */
  public void writeDerived(StatementWriter sql) {
    write(sql, false, false, false);
  }

  /** Tells if the query limits or skips rows. */
  public boolean paged() {
    return steps.paged();
  }

  private void write(StatementWriter sql, boolean keepOrder, boolean numbered, boolean json) {
    String alias = sql.alias();
    sql.scoped(alias, () -> {
      sql.append("SELECT ");
      if (steps.distinct()) {
        sql.append("DISTINCT ");
      }
      sql.dialect().paging().top(sql, steps.limit(), steps.offset());
      columns(sql, alias, numbered, json);
      sql.append(" FROM ").table(table).append(" AS ").append(alias);
      where(sql);
      if (!steps.groupBy().isEmpty()) {
        sql.append(" GROUP BY ");
        for (int i = 0; i < steps.groupBy().size(); i++) {
          sql.append(i == 0 ? "" : ", ").column(steps.groupBy().get(i));
        }
      }
      if (!steps.having().isEmpty()) {
        sql.append(" HAVING ").write(Filter.and(steps.having().toArray(Filter[]::new)));
      }
      boolean order = ordered() && (keepOrder || paged());
      if (order) {
        sql.append(" ORDER BY ");
        order(sql, false);
      }
      if (paged()) {
        sql.dialect().paging().paging(sql, steps.limit(), steps.offset(), order);
      }
    });
  }

  private void columns(StatementWriter sql, String alias, boolean numbered, boolean json) {
    List<Runnable> columns = new ArrayList<>();
    if (allColumns()) {
      columns.add(() -> sql.append(alias + ".*"));
    }
    steps.columns().forEach(name -> columns.add(() -> sql.column(name)));
    steps.values().forEach(value -> columns.add(() -> sql.write(value.value()).append(" AS ").name(value.name())));
    steps.includes().forEach(include -> columns.add(() -> sql.dialect().json().include(sql, include, json)));
    if (numbered) {
      columns.add(() -> {
        sql.append("ROW_NUMBER() OVER (ORDER BY ");
        order(sql, true);
        sql.append(") AS ").name(JsonSyntax.ROW_NUMBER);
      });
    }
    for (int i = 0; i < columns.size(); i++) {
      sql.append(i == 0 ? "" : ", ");
      columns.get(i).run();
    }
  }

  private void where(StatementWriter sql) {
    List<Filter> filters = new ArrayList<>();
    for (On pair : steps.on()) {
      filters.add(related -> related.append("(").column(pair.column()).append(" = ").outerColumn(pair.outerColumn())
          .append(")"));
    }
    filters.addAll(steps.filters());
    if (!filters.isEmpty()) {
      sql.append(" WHERE ").write(filters.size() == 1 ? filters.getFirst() : Filter.and(filters.toArray(Filter[]::new)));
    }
  }

  /** Writes the order; in a window, a column that the query gives is written as its value. */
  private void order(StatementWriter sql, boolean window) {
    List<Sort> order = steps.order();
    for (int i = 0; i < order.size(); i++) {
      Sort sort = order.get(i);
      sql.append(i == 0 ? "" : ", ");
      if (sort.value() != null) {
        sql.write(sort.value());
      } else {
        sortBy(sql, sort.name(), window);
      }
      sql.append(sort.direction() == Direction.DESC ? " DESC" : " ASC");
    }
  }

  private void sortBy(StatementWriter sql, String name, boolean window) {
    for (Value value : steps.values()) {
      if (value.name().equals(name)) {
        if (window) {
          sql.write(value.value());
        } else {
          sql.name(name);
        }
        return;
      }
    }
    for (JsonSyntax.Include include : steps.includes()) {
      if (include.name().equals(name)) {
        if (window) {
          throw new IllegalArgumentException("the related rows of an include are in the order of their own columns,"
              + " not of the include '" + name + "'");
        }
        sql.name(name);
        return;
      }
    }
    sql.column(name);
  }

  public static QueryImpl related(Query rows) {
    if (!(rows instanceof QueryImpl query)) {
      throw new IllegalArgumentException("the related rows are a query of a collection");
    }
    return query;
  }
}
