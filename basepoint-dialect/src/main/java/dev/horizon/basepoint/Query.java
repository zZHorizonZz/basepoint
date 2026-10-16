package dev.horizon.basepoint;

/**
 * A SELECT of the rows of one table. Create a query with {@link CollectionReference#query()}.
 *
 * <p>
 * Queries are immutable. Each method returns a new query, so you can store a query and reuse it.
 *
 * <p>
 * A query can also select related rows for {@link #include}, {@link #includeOne}, {@link #includeCount} and
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
   * Adds the number of related rows as a column.
   *
   * @param column the name of the result column, for example {@code item_count}
   * @param rows the query of the related rows. Use {@link #on} to link them to the current row.
   * @return a new query with the count
   */
  Query includeCount(String column, Query rows);

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
