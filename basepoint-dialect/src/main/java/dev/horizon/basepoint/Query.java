package dev.horizon.basepoint;

/**
 * A SELECT of the rows of one table. Create a query with {@link CollectionReference#query()}.
 *
 * <p>
 * Queries are immutable. Each method returns a new query, so you can store a query and reuse it.
 *
 * <p>
 * A query can also select related rows for {@link #include}, {@link #includeOne}, {@link Expression#subquery} and
 * {@link Filter#exists}. In that case, {@link #on} relates its rows to the current row of the enclosing query.
 */
public interface Query extends Filterable<Query>, Pageable<Query>, Combinable {

  /**
   * Selects only these columns. By default, a query selects all columns.
   *
   * @param columns the column names
   * @return a new query with the columns
   */
  Query columns(String... columns);

  /**
   * Selects a value as a named column, for example {@code select("revenue", sum("total"))}. If you add values and
   * want table columns too, list them with {@link #columns}.
   *
   * @param column the name of the result column
   * @param value the value
   * @return a new query with the value
   */
  Query select(String column, Expression value);

  /**
   * Selects the number of rows as the column {@code count}.
   *
   * @return a new query that counts rows
   */
  Query count();

  /**
   * Adds a JSON array of related rows as a column. The array keeps the sort order of {@code rows}.
   *
   * <p>
   * On MariaDB and SQLite, {@code rows} must list its columns with {@link #columns}.
   *
   * @param column the name of the result column, for example {@code items}
   * @param rows the query of the related rows. Use {@link #on} to link them to the current row.
   * @return a new query with the include
   */
  Query include(String column, Query rows);

  /**
   * Adds the first related row as a JSON object column. The column is {@code null} if there is no related row.
   *
   * <p>
   * On MariaDB and SQLite, {@code row} must list its columns with {@link #columns}.
   *
   * @param column the name of the result column, for example {@code customer}
   * @param row the query of the related rows. Use {@link #on} to link them to the current row.
   * @return a new query with the include
   */
  Query includeOne(String column, Query row);

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
   * @param rows the rows, a query or a compound query, without {@link #on}
   * @return a new query with the WITH query
   * @throws IllegalArgumentException if the query has a WITH query with the name, or the rows use {@link #on}
   */
  Query with(String name, Combinable rows);

  /**
   * Relates the rows of this query to the current row of the enclosing query. A row matches when its
   * {@code column} equals the enclosing row's {@code outerColumn}. Each call adds a pair of columns.
   *
   * @param column the column of this query's table, for example {@code order_id}
   * @param outerColumn the column of the enclosing query's table, for example {@code id}
   * @return a new query with the relation
   */
  Query on(String column, String outerColumn);

  /**
   * Groups rows by columns. Use it with aggregates in {@link #select}.
   *
   * @param columns the column names
   * @return a new query with the grouping
   */
  Query groupBy(String... columns);

  /**
   * Groups rows by values, such as the column of a relation: {@code groupBy(column("customer", "region"))}.
   *
   * @param values the values
   * @return a new query with the grouping
   */
  Query groupBy(Expression... values);

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
   * @param rows the related rows: a query with {@link #on}, which relates them to the row, and optionally a filter.
   * The outer column of {@code on} can be a path of an earlier relation, such as {@code customer.country_id}.
   * @return a new query with the relation
   * @throws IllegalArgumentException if the query has a relation of the name, or the rows have no {@code on} or have
   * anything other than {@code on} and filters
   */
  Query relation(String name, Query rows);

  /**
   * Adds a filter on groups, for example {@code having(gt(sum("total"), 1000))}.
   *
   * @param filter the filter
   * @return a new query with the filter
   */
  Query having(Filter filter);

  /**
   * Sorts by a value. To sort by a column from {@link #select}, pass its name to {@link #orderBy(String)}.
   *
   * @param value the value to sort by
   * @param direction the sort direction
   * @return a new query with the sort key
   */
  Query orderBy(Expression value, Direction direction);

  /**
   * Locks the rows that the query reads, until the transaction of the query ends.
   *
   * @param mode the lock
   * @return a new query with the lock
   */
  Query lock(LockMode mode);

  /**
   * Removes duplicate rows ({@code SELECT DISTINCT}).
   *
   * @return a new query that returns distinct rows
   */
  Query distinct();
}
