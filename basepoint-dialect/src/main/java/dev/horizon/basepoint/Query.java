package dev.horizon.basepoint;

import dev.horizon.basepoint.expression.As;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Reference;
import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.filter.Related;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.From;
import dev.horizon.basepoint.statement.GroupBy;
import dev.horizon.basepoint.statement.Having;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Join;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Lock;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.Operand;
import dev.horizon.basepoint.statement.OrderBy;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.SetOperator;
import dev.horizon.basepoint.statement.Sort;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Where;
import dev.horizon.basepoint.statement.With;
import dev.horizon.basepoint.statement.WithQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A SELECT of rows: the rows of one table, or the rows of queries combined with {@link #union}, {@link #intersect} or
 * {@link #except}. Create a query with {@link CollectionReference#query()}.
 *
 * <p>
 * Queries are immutable. Each method returns a new query, so you can store a query and reuse it.
 *
 * <p>
 * A query can also select related rows for {@link #include}, {@link #includeOne}, {@link Expression#subquery} and
 * {@link Filter#exists}. In that case, {@link #on} relates its rows to the current row of the enclosing query.
 */
public final class Query {

  private static final List<Class<? extends Clause>> ORDER = List.of(With.class, Select.class, From.class, Join.class,
      Where.class, GroupBy.class, Having.class, OrderBy.class, Limit.class, Offset.class, Lock.class);

  private final Dialect dialect;
  private final Statement statement;
  private final List<Filter> on;

  private Query(Dialect dialect, Statement statement, List<Filter> on) {
    this.dialect = dialect;
    this.statement = statement;
    this.on = List.copyOf(on);
  }

  Query(Dialect dialect, String table) {
    this(dialect, new Statement(new Source.Table(table), List.of(new Select(false, List.of()), new From())), List.of());
  }

  /**
   * Adds a filter. Each call adds a filter, and a row must match all of them.
   *
   * @param filter the filter
   * @return a new query with the filter
   */
  public Query where(Filter filter) {
    return edit(rows -> rows.where(filter));
  }

  /**
   * Adds a filter written in the AIP-160 filter syntax. See {@link Filter#of(String)}. A field of two parts, such as
   * {@code customer.region}, reads the column of a relation when the query has a relation of the first part, and a
   * key of the JSON object in a column otherwise.
   *
   * @param text the filter text, for example {@code state = "OPEN" AND total > 100}
   * @return a new query with the filter
   */
  public Query where(String text) {
    return where(Filter.of(text));
  }

  /**
   * Selects only these columns. By default, a query selects all columns.
   *
   * @param columns the column names
   * @return a new query with the columns
   */
  public Query columns(String... columns) {
    return select(Arrays.stream(columns).map(Expression::column).toList());
  }

  /**
   * Selects a value as a named column, for example {@code select("revenue", sum("total"))}. If you add values and
   * want table columns too, list them with {@link #columns}.
   *
   * @param column the name of the result column
   * @param value the value
   * @return a new query with the value
   */
  public Query select(String column, Expression value) {
    return select(List.of(new As(value, column)));
  }

  /**
   * Selects the number of rows as the column {@code count}.
   *
   * @return a new query that counts rows
   */
  public Query count() {
    return select("count", Expression.count());
  }

  /**
   * Adds a JSON array of related rows as a column. The array keeps the sort order of {@code rows}.
   *
   * <p>
   * On MariaDB and SQLite, {@code rows} must list its columns with {@link #columns}.
   *
   * @param column the name of the result column, for example {@code items}
   * @param rows the query of the related rows. Use {@link #on} to link them to the current row.
   * @return a new query with the include
   * @throws InvalidQueryException if the rows select a column named {@value Include#ROW_NUMBER} or have WITH queries
   */
  public Query include(String column, Query rows) {
    return select(column, new Include.Many(rows.statement()));
  }

  /**
   * Adds the first related row as a JSON object column. The column is {@code null} if there is no related row.
   *
   * <p>
   * On MariaDB and SQLite, {@code row} must list its columns with {@link #columns}.
   *
   * @param column the name of the result column, for example {@code customer}
   * @param row the query of the related rows. Use {@link #on} to link them to the current row.
   * @return a new query with the include
   * @throws InvalidQueryException if the rows have WITH queries
   */
  public Query includeOne(String column, Query row) {
    return select(column, new Include.One(row.limit(1).statement()));
  }

  /**
   * Adds a WITH query: {@code WITH name AS (rows)}, which this query and the queries in it read as a table named
   * {@code name}, for example {@code dialect.collection("big").query().with("big", orders.query().where(gt("total",
   * 100)))}. A WITH query can read the WITH queries that come before it.
   *
   * <p>
   * Call it on the outermost query: a query that is part of another statement, such as an include or a subquery,
   * cannot have its own WITH queries, because SQL Server allows {@code WITH} only at the start of a statement. The
   * rows keep their sort order only with a limit or offset.
   *
   * @param name the name of the WITH query
   * @param rows the rows, without {@link #on}
   * @return a new query with the WITH query
   * @throws InvalidQueryException if the query has a WITH query with the name, or the rows use {@link #on}
   */
  public Query with(String name, Query rows) {
    if (!rows.on.isEmpty()) {
      throw new InvalidQueryException("The WITH query " + name + " relates its rows with on(), but a WITH query has"
          + " no enclosing row. Leave out on().");
    }
    return with(new WithQuery(name, rows.combines() ? rows.statement : rows.unordered(), false));
  }

  /**
   * Adds a recursive WITH query: {@code WITH RECURSIVE name AS (start UNION ALL step)}. The step reads the rows that
   * the query has so far through a relation to it, so each pass adds the rows that follow from the last ones, such as
   * the children of the categories found before:
   *
   * <pre>{@code
   * Query start = categories.query().columns("id").select("depth", value(0)).where(eq("id", 1));
   * Query step = categories.query().relation("parent", dialect.collection("tree").query().on("id", "parent_id"))
   *     .columns("id").select("depth", add(column("parent", "depth"), value(1)));
   * dialect.collection("tree").query().withRecursive("tree", start, step);
   * }</pre>
   *
   * <p>
   * The start and the step select the same columns, in the same order. A relation of the step to the WITH query is an
   * inner join, since the databases allow the recursive reference only there. A limit of the depth is the job of the
   * step, such as {@code where(lt(column("parent", "depth"), 10))}: a cycle in the data recurses until the database
   * stops it. SQL Server stops after 100 levels.
   *
   * @param name the name of the WITH query
   * @param start the first rows, which do not read the WITH query
   * @param step the rows that follow from the rows so far, which read the WITH query through a relation
   * @return a new query with the WITH query
   * @throws InvalidQueryException if the start reads the WITH query, the step does not, or one of them has
   * {@link #on}, a limit or an offset
   */
  public Query withRecursive(String name, Query start, Query step) {
    start.requirePart(name, "start");
    step.requirePart(name, "step");
    if (start.reads(name)) {
      throw new InvalidQueryException("The start of the recursive WITH query " + name + " reads it. Only the step reads"
          + " it, through a relation.");
    }
    if (!step.reads(name)) {
      throw new InvalidQueryException("The step of the recursive WITH query " + name + " does not read it. Read it"
          + " through a relation, such as relation(\"parent\", dialect.collection(\"" + name + "\").query().on(\"id\","
          + " \"parent_id\")).");
    }
    Statement recursion = step.unordered();
    for (Join join : recursion.findAll(Join.class)) {
      recursion = join.table().equals(name) ? recursion.replace(join, join.toInner()) : recursion;
    }
    Statement rows = new Statement(new Source.None(), List.of(new Operand(start.unordered()), SetOperator.UNION_ALL,
        new Operand(recursion)));
    return with(new WithQuery(name, rows, true));
  }

  /**
   * Relates the rows of this query to the current row of the enclosing query. A row matches when its
   * {@code column} equals the enclosing row's {@code outerColumn}. Each call adds a pair of columns.
   *
   * @param column the column of this query's table, for example {@code order_id}
   * @param outerColumn the column of the enclosing query's table, for example {@code id}
   * @return a new query with the relation
   */
  public Query on(String column, String outerColumn) {
    List<Filter> more = new ArrayList<>(on);
    more.add(new Related(column, outerColumn));
    return new Query(dialect, statement, more);
  }

  /**
   * Groups rows by columns. Use it with aggregates in {@link #select}.
   *
   * @param columns the column names
   * @return a new query with the grouping
   */
  public Query groupBy(String... columns) {
    return groupBy(Arrays.stream(columns).map(Expression::column).toArray(Expression[]::new));
  }

  /**
   * Groups rows by values, such as the column of a relation: {@code groupBy(column("customer", "region"))}.
   *
   * @param values the values
   * @return a new query with the grouping
   */
  public Query groupBy(Expression... values) {
    return edit(rows -> {
      List<Expression> all = new ArrayList<>(rows.find(GroupBy.class).map(GroupBy::columns).orElse(List.of()));
      all.addAll(List.of(values));
      return rows.put(new GroupBy(all), ORDER);
    });
  }

  /**
   * Names the related row of each row, which paths read: {@code column(name, column)} in {@link #select}, filters,
   * {@link #orderBy(Expression, Direction)} and {@link #groupBy(Expression...)}. The query writes a {@code LEFT JOIN}
   * for each relation, so a row whose related row does not exist stays, and its paths read {@code NULL}.
   *
   * <p>
   * A relation gives at most one row for each row, such as the customer of an order. A relation of many rows would
   * repeat each row once for each related row: read related rows of many with {@link #include}, {@link Filter#exists}
   * or {@link Expression#subquery}.
   *
   * @param name the name of the relation, which paths read
   * @param rows the related rows: a query of a table with {@link #on}, which relates them to the row, and optionally
   * a filter. The outer column of {@code on} can be a path of an earlier relation, such as
   * {@code customer.country_id}.
   * @return a new query with the relation
   * @throws InvalidQueryException if the query has a relation of the name, or the rows have no {@code on} or have
   * anything other than {@code on} and filters
   */
  public Query relation(String name, Query rows) {
    Statement plain = rows.statement.find(Where.class).map(rows.statement::remove).orElse(rows.statement);
    if (!(plain.source() instanceof Source.Table table) || !plain.equals(new Query(dialect, table.name()).statement)) {
      throw new InvalidQueryException("The rows of the relation " + name + " can have only on() and filters. A"
          + " relation of selected, grouped, sorted or limited rows is not supported.");
    }
    if (rows.on.isEmpty()) {
      throw new InvalidQueryException("The rows of the relation " + name + " have no on(), so every row would relate"
          + " to every row. Relate them with on(column, outerColumn).");
    }
    List<Join> joins = statement.findAll(Join.class);
    if (joins.stream().anyMatch(join -> join.name().equals(name))) {
      throw new InvalidQueryException("The query has two relations named " + name + ".");
    }
    for (Filter filter : rows.on) {
      if (filter instanceof Related(String _, String outer) && outer.indexOf('.') > 0 && joins.stream().noneMatch(
          join -> join.name().equals(outer.substring(0, outer.indexOf('.'))))) {
        throw new InvalidQueryException("The outer column " + outer + " reads the relation " + outer.substring(0,
            outer.indexOf('.')) + ", which the query does not name before it.");
      }
    }
    return add(new Join(name, table.name(), rows.statement().find(Where.class).orElseThrow().filter(), false));
  }

  /**
   * Adds a filter on groups, for example {@code having(gt(sum("total"), 1000))}.
   *
   * @param filter the filter
   * @return a new query with the filter
   */
  public Query having(Filter filter) {
    return edit(rows -> rows.put(new Having(rows.find(Having.class).map(having -> Filter.and(having.filter(), filter))
        .orElse(filter)), ORDER));
  }

  /**
   * Sorts by a column in ascending order.
   *
   * @param column the column name, of the table or of {@link #select}
   * @return a new query with the sort key
   */
  public Query orderBy(String column) {
    return orderBy(column, Direction.ASC);
  }

  /**
   * Sorts by a column. Each call adds a sort key after the existing ones.
   *
   * @param column the column name, of the table or of {@link #select}
   * @param direction the sort direction
   * @return a new query with the sort key
   */
  public Query orderBy(String column, Direction direction) {
    return orderBy(new Reference(column), direction);
  }

  /**
   * Sorts by a value. Each call adds a sort key after the existing ones.
   *
   * @param value the value to sort by
   * @param direction the sort direction
   * @return a new query with the sort key
   */
  public Query orderBy(Expression value, Direction direction) {
    return edit(rows -> {
      List<Sort> sorts = new ArrayList<>(rows.find(OrderBy.class).map(OrderBy::sorts).orElse(List.of()));
      sorts.add(new Sort(value, direction));
      return rows.put(new OrderBy(sorts), ORDER);
    });
  }

  /**
   * Limits the number of rows.
   *
   * @param rows the maximum number of rows, which must be greater than 0
   * @return a new query with the limit
   * @throws IllegalArgumentException if {@code rows} is 0 or less
   */
  public Query limit(long rows) {
    return put(new Limit(rows));
  }

  /**
   * Skips a number of rows.
   *
   * @param rows the number of rows to skip, which must be 0 or greater
   * @return a new query with the offset
   * @throws IllegalArgumentException if {@code rows} is less than 0
   */
  public Query offset(long rows) {
    return put(new Offset(rows));
  }

  /**
   * Removes duplicate rows: {@code SELECT DISTINCT}, or {@code UNION} instead of {@code UNION ALL}.
   *
   * @return a new query that returns distinct rows
   */
  public Query distinct() {
    if (combines()) {
      Statement rows = statement;
      for (SetOperator operator : statement.findAll(SetOperator.class)) {
        rows = operator == SetOperator.UNION_ALL ? rows.replace(operator, SetOperator.UNION) : rows;
      }
      return new Query(dialect, rows, on);
    }
    return edit(rows -> {
      Select select = rows.find(Select.class).orElseThrow();
      return rows.replace(select, select.distinct(true));
    });
  }

  /**
   * Locks the rows that the query reads, until the transaction of the query ends.
   *
   * @param mode the lock
   * @return a new query with the lock
   */
  public Query lock(LockMode mode) {
    return put(new Lock(mode));
  }

  /**
   * Returns the rows of both queries, including duplicates ({@code UNION ALL}). Call {@link #distinct()} on the result
   * to remove duplicates. Sorts, limits and filters of the result apply to the combined rows.
   *
   * @param rows the query whose rows combine with these, which selects the same columns
   * @return a new query of the combined rows
   * @throws InvalidQueryException if the queries come from different dialects
   */
  public Query union(Query rows) {
    return combine(SetOperator.UNION_ALL, rows);
  }

  /**
   * Returns the distinct rows that both queries return ({@code INTERSECT}).
   *
   * @param rows the query whose rows combine with these, which selects the same columns
   * @return a new query of the combined rows
   * @throws InvalidQueryException if the queries come from different dialects
   */
  public Query intersect(Query rows) {
    return combine(SetOperator.INTERSECT, rows);
  }

  /**
   * Returns the distinct rows of this query that the other query does not return ({@code EXCEPT}).
   *
   * @param rows the query whose rows to leave out, which selects the same columns
   * @return a new query of the combined rows
   * @throws InvalidQueryException if the queries come from different dialects
   */
  public Query except(Query rows) {
    return combine(SetOperator.EXCEPT, rows);
  }

  /**
   * Returns the statement tree, before any decorator rewrites it.
   *
   * @return the statement
   */
  public Statement statement() {
    if (on.isEmpty()) {
      return statement;
    }
    return statement.find(Where.class).map(where -> {
      List<Filter> filters = new ArrayList<>(on);
      filters.add(where.filter());
      return statement.replace(where, new Where(Filter.and(filters)));
    }).orElseGet(() -> statement.where(Filter.and(on)));
  }

  /**
   * Renders the query in the dialect of the collection that created it.
   *
   * @return the SQL text and its placeholder values
   */
  public RenderedStatement build() {
    return dialect.render(statement());
  }

  private Query select(List<Expression> columns) {
    return edit(rows -> {
      Select select = rows.find(Select.class).orElseThrow();
      return rows.replace(select, select.add(columns));
    });
  }

  private Query with(WithQuery query) {
    return edit(rows -> {
      List<WithQuery> queries = new ArrayList<>(rows.find(With.class).map(With::queries).orElse(List.of()));
      if (queries.stream().anyMatch(earlier -> earlier.name().equals(query.name()))) {
        throw new InvalidQueryException("The query has two WITH queries named " + query.name() + ".");
      }
      queries.add(query);
      return rows.put(new With(queries), ORDER);
    });
  }

  private Query combine(SetOperator operator, Query rows) {
    if (!dialect.equals(rows.dialect)) {
      throw new InvalidQueryException("The combined queries come from different dialects, and the statement renders"
          + " with one. Read both from collections of the same dialect.");
    }
    List<Clause> clauses = new ArrayList<>();
    if (combines() && statement.findAll(SetOperator.class).stream().allMatch(operator::equals)) {
      clauses.addAll(statement.clauses());
    } else {
      clauses.add(new Operand(operand()));
    }
    clauses.add(operator);
    clauses.add(new Operand(rows.operand()));
    return new Query(dialect, new Statement(new Source.None(), clauses), List.of());
  }

  private Query put(Clause clause) {
    return edit(rows -> rows.put(clause, ORDER));
  }

  private Query add(Clause clause) {
    return edit(rows -> rows.add(clause, ORDER));
  }

  private Query edit(UnaryOperator<Statement> change) {
    Statement rows = combines() ? derive(statement) : statement;
    return new Query(dialect, change.apply(rows), on);
  }

  private boolean combines() {
    return statement.has(SetOperator.class);
  }

  private boolean pages() {
    return statement.has(Limit.class) || statement.has(Offset.class);
  }

  private Statement operand() {
    return combines() || pages() ? derive(statement()) : unordered();
  }

  private Statement unordered() {
    Statement rows = statement();
    return pages() ? rows : rows.find(OrderBy.class).map(rows::remove).orElse(rows);
  }

  private Statement derive(Statement rows) {
    return new Statement(new Source.Rows(rows), List.of(new Select(false, List.of()), new From()));
  }

  private void requirePart(String name, String role) {
    if (!on.isEmpty() || pages()) {
      throw new InvalidQueryException("The " + role + " of the recursive WITH query " + name + " has on(), a limit or"
          + " an offset, which a part of a recursive WITH query cannot have.");
    }
  }

  private boolean reads(String name) {
    return Objects.equals(statement.source(), new Source.Table(name)) || statement.findAll(Join.class).stream()
        .anyMatch(join -> join.table().equals(name));
  }
}
