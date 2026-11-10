package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Combinable;
import dev.horizon.basepoint.CompoundQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.Include;
import dev.horizon.basepoint.LockMode;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The default {@link Query}.
 *
 * <p>
 * Besides its own statement, a query builds the statements that other statements embed: included rows, counts of
 * related rows, {@code EXISTS} subqueries and compound query operands.
 */
public final class QueryImpl implements Query {

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
        List.of(), false, null, null, null, List.of(), List.of()));
  }

  /**
   * The state of a query.
   *
   * @param columns the columns that it names
   * @param values the values that it gives as columns
   * @param includes the related rows that it gives as columns
   * @param on the filters that relate the rows to the row around them
   * @param filters the filters of WHERE
   * @param groupBy the columns of the groups
   * @param having the filters of the groups
   * @param order the order
   * @param distinct true to give each row once
   * @param limit the largest number of rows, or null for all
   * @param offset the number of rows to skip, or null for none
   * @param lock the lock of the rows, or null for none
   */
  private record Steps(List<String> columns, List<Named> values, List<Named> includes, List<Filter> on,
      List<Filter> filters, List<Expression> groupBy, List<Filter> having, List<Order> order, boolean distinct, Long limit,
      Long offset, LockMode lock, List<Clause.WithQuery> queries, List<Relation> relations) {

    Steps withColumns(List<String> more) {
      return new Steps(append(columns, more), values, includes, on, filters, groupBy, having, order, distinct, limit,
          offset, lock, queries, relations);
    }

    Steps withValue(Named value) {
      return new Steps(columns, append(values, List.of(value)), includes, on, filters, groupBy, having, order,
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withInclude(Named include) {
      return new Steps(columns, values, append(includes, List.of(include)), on, filters, groupBy, having, order,
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withOn(Filter related) {
      return new Steps(columns, values, includes, append(on, List.of(related)), filters, groupBy, having, order,
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withFilter(Filter filter) {
      return new Steps(columns, values, includes, on, append(filters, List.of(filter)), groupBy, having, order,
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withGroupBy(List<Expression> more) {
      return new Steps(columns, values, includes, on, filters, append(groupBy, more), having, order, distinct, limit,
          offset, lock, queries, relations);
    }

    Steps withHaving(Filter filter) {
      return new Steps(columns, values, includes, on, filters, groupBy, append(having, List.of(filter)), order,
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withOrder(Order more) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, append(order, List.of(more)),
          distinct, limit, offset, lock, queries, relations);
    }

    Steps withDistinct() {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, true, limit, offset, lock, queries,
          relations);
    }

    Steps withLimit(Long rows) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, rows, offset, lock, queries,
          relations);
    }

    Steps withOffset(Long rows) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, limit, rows, lock, queries,
          relations);
    }

    Steps withLock(LockMode mode) {
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, limit, offset, mode, queries,
          relations);
    }

    Steps withRelation(Relation relation) {
      if (relations.stream().anyMatch(other -> other.name().equals(relation.name()))) {
        throw new IllegalArgumentException("The query has two relations named " + relation.name() + ".");
      }
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, limit, offset, lock,
          queries, append(relations, List.of(relation)));
    }

    Steps withQuery(Clause.WithQuery query) {
      if (queries.stream().anyMatch(other -> other.name().equals(query.name()))) {
        throw new IllegalArgumentException("The query has two WITH queries named " + query.name() + ".");
      }
      return new Steps(columns, values, includes, on, filters, groupBy, having, order, distinct, limit, offset, lock,
          append(queries, List.of(query)), relations);
    }

    private static <T> List<T> append(List<T> list, List<T> items) {
      List<T> more = new ArrayList<>(list);
      more.addAll(items);
      return List.copyOf(more);
    }
  }

  /**
   * A named value in the SELECT list.
   *
   * @param name the name of the column
   * @param value the value
   */
  private record Named(String name, Expression value) {
  }

  /**
   * A sort key, by name or by value.
   *
   * @param name the name, or null
   * @param value the value, or null
   * @param direction the direction
   */
  private record Order(String name, Expression value, Direction direction) {
  }

  /** A relation of the query: its table and the condition of its join. */
  private record Relation(String name, String table, Filter condition) {
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
    return with(steps.withValue(new Named(column, value)));
  }

  @Override
  public Query count() {
    return select("count", Expression.count());
  }

  @Override
  public Query include(String column, Query rows) {
    return with(steps.withInclude(new Named(column, new Include(Include.Shape.MANY, related(rows).statement()))));
  }

  @Override
  public Query includeOne(String column, Query row) {
    return with(steps.withInclude(new Named(column,
        new Include(Include.Shape.ONE, related(row.limit(1)).statement()))));
  }

  @Override
  public Query with(String name, Combinable rows) {
    Statement statement = switch (rows) {
      case QueryImpl query when !query.steps.on().isEmpty() -> throw new IllegalArgumentException("The WITH query "
          + name + " relates its rows with on(), but a WITH query has no enclosing row. Leave out on().");
      case QueryImpl query -> query.derived();
      case CompoundQueryImpl compound -> compound.statement();
      default -> throw new IllegalArgumentException("The rows of a WITH query must be a query or a compound query"
          + " from this library, but are " + rows.getClass().getName() + ".");
    };
    return with(steps.withQuery(new Clause.WithQuery(name, statement, false)));
  }

  @Override
  public Query withRecursive(String name, Query start, Query step) {
    QueryImpl first = part(name, "start", start);
    QueryImpl next = part(name, "step", step);
    if (first.reads(name)) {
      throw new IllegalArgumentException("The start of the recursive WITH query " + name + " reads it. Only the step"
          + " reads it, through a relation.");
    }
    if (!next.reads(name)) {
      throw new IllegalArgumentException("The step of the recursive WITH query " + name + " does not read it. Read it"
          + " through a relation, such as relation(\"parent\", dialect.collection(\"" + name + "\").query().on(\"id\","
          + " \"parent_id\")).");
    }
    Statement rows = new Statement(null, List.of(new Clause.Operand(first.derived()), new Clause.SetOperator(
        "UNION ALL"), new Clause.Operand(next.nested().statement(false, Set.of(name)))));
    return with(steps.withQuery(new Clause.WithQuery(name, rows, true)));
  }

  /** Gives a part of a recursive WITH query, which relates to no outer row and has no limit or offset. */
  private QueryImpl part(String name, String role, Query rows) {
    if (!(rows instanceof QueryImpl query)) {
      throw new IllegalArgumentException("The " + role + " of the recursive WITH query " + name + " must be a query"
          + " from a collection.");
    }
    if (!query.steps.on().isEmpty() || query.paged()) {
      throw new IllegalArgumentException("The " + role + " of the recursive WITH query " + name + " has on(), a limit"
          + " or an offset, which a part of a recursive WITH query cannot have.");
    }
    return query;
  }

  /** Tells if this query reads a table, as its source or as the table of a relation. */
  private boolean reads(String name) {
    return table.equals(name) || steps.relations().stream().anyMatch(relation -> relation.table().equals(name));
  }

  @Override
  public Query on(String column, String outerColumn) {
    return with(steps.withOn(new Filter.Related(column, outerColumn)));
  }

  @Override
  public Query groupBy(String... columns) {
    return with(steps.withGroupBy(Arrays.stream(columns).map(Expression::column).toList()));
  }

  @Override
  public Query groupBy(Expression... values) {
    return with(steps.withGroupBy(List.of(values)));
  }

  @Override
  public Query relation(String name, Query rows) {
    if (!(rows instanceof QueryImpl related)) {
      throw new IllegalArgumentException("The rows of the relation " + name + " must be a query from a collection.");
    }
    Steps other = related.steps;
    boolean plain = other.columns().isEmpty() && other.values().isEmpty() && other.includes().isEmpty()
        && other.groupBy().isEmpty() && other.having().isEmpty() && other.order().isEmpty() && !other.distinct()
        && other.limit() == null && other.offset() == null && other.lock() == null && other.queries().isEmpty()
        && other.relations().isEmpty();
    if (!plain) {
      throw new IllegalArgumentException("The rows of the relation " + name + " can have only on() and filters. A"
          + " relation of selected, grouped, sorted or limited rows is not supported.");
    }
    if (other.on().isEmpty()) {
      throw new IllegalArgumentException("The rows of the relation " + name + " have no on(), so every row would"
          + " relate to every row. Relate them with on(column, outerColumn).");
    }
    List<Filter> condition = new ArrayList<>(other.on());
    condition.addAll(other.filters());
    related.requireEarlier(steps.relations());
    return with(steps.withRelation(new Relation(name, related.table, and(condition))));
  }

  /** Checks that each path in the outer columns of {@code on} reads a relation that the query names before. */
  private void requireEarlier(List<Relation> earlier) {
    for (Filter filter : steps.on()) {
      if (filter instanceof Filter.Related(String _, String outer) && outer.indexOf('.') > 0) {
        String relation = outer.substring(0, outer.indexOf('.'));
        if (earlier.stream().noneMatch(other -> other.name().equals(relation))) {
          throw new IllegalArgumentException("The outer column " + outer + " reads the relation " + relation
              + ", which the query does not name before it.");
        }
      }
    }
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
    return with(steps.withOrder(new Order(column, null, direction)));
  }

  @Override
  public Query orderBy(Expression value, Direction direction) {
    return with(steps.withOrder(new Order(null, value, direction)));
  }

  @Override
  public Query limit(long rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("The limit must be greater than 0, but is " + rows + ".");
    }
    return with(steps.withLimit(rows));
  }

  @Override
  public Query offset(long rows) {
    if (rows < 0) {
      throw new IllegalArgumentException("The offset must be 0 or greater, but is " + rows + ".");
    }
    return with(steps.withOffset(rows));
  }

  @Override
  public Query distinct() {
    return with(steps.withDistinct());
  }

  @Override
  public Query lock(LockMode mode) {
    return with(steps.withLock(Objects.requireNonNull(mode, "mode")));
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
  public Statement statement() {
    return statement(true);
  }

  @Override
  public RenderedStatement build() {
    return new StatementWriter(dialect).render(statement()).rendered();
  }

  /**
   * Returns the dialect of this query.
   *
   * @return the dialect
   */
  public Dialect dialect() {
    return dialect;
  }

  /**
   * Returns whether this query has a limit or an offset.
   *
   * @return true if it does
   */
  public boolean paged() {
    return steps.limit() != null || steps.offset() != null;
  }

  /**
   * Returns the statement for use as a compound query operand. The sort order is kept only with a limit or offset.
   *
   * @return the statement
   */
  public Statement derived() {
    return nested().statement(false);
  }

  /**
   * Returns {@code SELECT 1} over the filtered rows, for {@code EXISTS}.
   *
   * @return the statement
   */
  public Statement exists() {
    return select(new Expression.Literal(1));
  }

  /**
   * Returns this query as one value of an enclosing statement.
   *
   * @return the statement
   * @throws IllegalArgumentException if the query selects all columns or more than one
   */
  public Statement value() {
    int selected = steps.columns().size() + steps.values().size() + steps.includes().size();
    if (selected != 1) {
      throw new IllegalArgumentException("A subquery selects one column, but this query selects " + (selected == 0
          ? "all columns"
          : selected + " columns") + ". Select one with count(), select(name, value) or columns(name).");
    }
    return statement();
  }

  /** Returns a SELECT of one value over the filtered rows. */
  private Statement select(Expression value) {
    List<Clause> clauses = new ArrayList<>();
    clauses.add(new Clause.Select(false, List.of(value)));
    clauses.add(new Clause.From());
    clauses.addAll(joins(Set.of()));
    where().ifPresent(clauses::add);
    return new Statement(Source.aliased(table), clauses);
  }

  private Statement statement(boolean keepOrder) {
    return statement(keepOrder, Set.of());
  }

  /** Builds the statement, with an inner join for each relation to one of the tables of {@code inner}. */
  private Statement statement(boolean keepOrder, Set<String> inner) {
    List<Expression> columns = new ArrayList<>();
    steps.columns().forEach(name -> columns.add(Expression.column(name)));
    steps.values().forEach(value -> columns.add(new Expression.As(value.value(), value.name())));
    steps.includes().forEach(include -> columns.add(new Expression.As(include.value(), include.name())));

    List<Clause> clauses = new ArrayList<>();
    if (!steps.queries().isEmpty()) {
      clauses.add(new Clause.With(steps.queries()));
    }
    clauses.add(new Clause.Select(steps.distinct(), columns));
    clauses.add(new Clause.From());
    clauses.addAll(joins(inner));
    where().ifPresent(clauses::add);
    if (!steps.groupBy().isEmpty()) {
      clauses.add(new Clause.GroupBy(steps.groupBy()));
    }
    if (!steps.having().isEmpty()) {
      clauses.add(new Clause.Having(and(steps.having())));
    }
    if (!steps.order().isEmpty() && (keepOrder || paged())) {
      clauses.add(new Clause.OrderBy(steps.order().stream().map(this::sort).toList()));
    }
    if (steps.limit() != null) {
      clauses.add(new Clause.Limit(steps.limit()));
    }
    if (steps.offset() != null) {
      clauses.add(new Clause.Offset(steps.offset()));
    }
    if (steps.lock() != null) {
      clauses.add(new Clause.Lock(steps.lock()));
    }
    return new Statement(Source.aliased(table), clauses);
  }

  private Optional<Clause> where() {
    List<Filter> filters = new ArrayList<>(steps.on());
    filters.addAll(steps.filters());
    return filters.isEmpty() ? Optional.empty() : Optional.of(new Clause.Where(and(filters)));
  }

  /** Returns a sort key. A name from the SELECT list renders unqualified; any other name is a table column. */
  /** Gives the joins of the relations, in their order, with an inner join for a relation to one of the tables. */
  private List<Clause> joins(Set<String> inner) {
    return steps.relations().stream().<Clause> map(relation -> new Clause.Join(relation.name(), relation.table(),
        relation.condition(), inner.contains(relation.table()))).toList();
  }

  private Clause.Sort sort(Order order) {
    if (order.value() != null) {
      return new Clause.Sort(order.value(), order.direction());
    }
    boolean given = steps.values().stream().anyMatch(value -> value.name().equals(order.name()))
        || steps.includes().stream().anyMatch(include -> include.name().equals(order.name()));
    return new Clause.Sort(given ? new Expression.Name(order.name()) : Expression.column(order.name()),
        order.direction());
  }

  /**
   * Combines filters with {@code AND}. A single filter is returned unchanged.
   *
   * @param filters the filters, at least one
   * @return the filter
   */
  public static Filter and(List<Filter> filters) {
    return filters.size() == 1 ? filters.getFirst() : Filter.and(filters.toArray(Filter[]::new));
  }

  /**
   * Returns a query as its implementation.
   *
   * @param rows the query
   * @return the query
   * @throws IllegalArgumentException if it is no query of a collection
   */
  public static QueryImpl related(Query rows) {
    if (!(rows instanceof QueryImpl query)) {
      throw new IllegalArgumentException("The related rows must be a query from a collection.");
    }
    return query.nested();
  }

  /** Returns this query, which is part of another statement, so it can have no WITH queries of its own. */
  private QueryImpl nested() {
    if (!steps.queries().isEmpty()) {
      throw new IllegalArgumentException("A query that is part of another statement cannot have with(), because SQL"
          + " Server allows WITH only at the start of a statement. Call with() on the outermost query.");
    }
    return this;
  }
}
