package dev.horizon.basepoint;

/**
 * A read of the rows of one table, from {@code query()} of a collection.
 *
 * <p>
 * Each step gives a new query and does not change the query it was called on, so a field can keep a query and give it
 * out. A query also chooses the related rows of another query: those of {@link #include} and of
 * {@link Filter#exists}, which {@link #on} relates to the row around them.
 */
public interface Query extends Filterable<Query>, Pageable<Query>, Combinable {

  /**
   * Gives these columns of the rows, instead of all of them.
   *
   * @param columns the names of the columns
   * @return the query that gives the columns
   */
  Query columns(String... columns);

  /**
   * Gives a value as a column of the rows, such as an aggregate: {@code select("revenue", sum("total"))}. A query of
   * values gives the other columns only if {@link #columns} names them.
   *
   * @param column the name of the column
   * @param value the value
   * @return the query that gives the value
   */
  Query select(String column, Expression value);

  /**
   * Gives the number of rows, as the column {@code count}.
   *
   * @return the query of the number
   */
  Query count();

  /**
   * Gives the related rows of another query as a column of each row: a JSON array of objects, in the order of that
   * query. MySQL and SQLite make JSON of the columns that the related query names.
   *
   * @param column the name of the column, for example {@code items}
   * @param rows the related rows, which {@link #on} relates to the row
   * @return the query that includes the rows
   */
  Query include(String column, Query rows);

  /**
   * Gives the first related row of another query as a column of each row: a JSON object, or null if there is none.
   *
   * @param column the name of the column, for example {@code customer}
   * @param row the related rows, which {@link #on} relates to the row
   * @return the query that includes the row
   */
  Query includeOne(String column, Query row);

  /**
   * Gives the number of the related rows of another query as a column of each row.
   *
   * @param column the name of the column, for example {@code item_count}
   * @param rows the related rows, which {@link #on} relates to the row
   * @return the query that counts the rows
   */
  Query includeCount(String column, Query rows);

  /**
   * Relates these rows to the row around them, in an include or in {@link Filter#exists}: a column of these rows
   * equals a column of that row. Two pairs must both be equal.
   *
   * @param column the column of these rows, for example {@code order_id}
   * @param outerColumn the column of the row around them, for example {@code id}
   * @return the query of the related rows
   */
  Query on(String column, String outerColumn);

  /**
   * Groups the rows by columns, for the aggregates of {@link #select}.
   *
   * @param columns the names of the columns
   * @return the query of the groups
   */
  Query groupBy(String... columns);

  /**
   * Keeps the groups that a filter of aggregates matches: {@code having(gt(sum("total"), 1000))}.
   *
   * @param filter the filter
   * @return the query that keeps the groups
   */
  Query having(Filter filter);

  /**
   * Orders the rows by a value. A name of {@link #orderBy(String)} can also be a column of {@link #select}.
   *
   * @param value the value
   * @param direction the direction
   * @return the query with the order
   */
  Query orderBy(Expression value, Direction direction);

  /**
   * Gives each row once.
   *
   * @return the query of distinct rows
   */
  Query distinct();
}
