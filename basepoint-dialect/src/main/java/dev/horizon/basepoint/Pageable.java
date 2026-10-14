package dev.horizon.basepoint;

/**
 * A read that supports sorting, limits and offsets.
 *
 * @param <S> the builder type that each method returns
 */
public interface Pageable<S extends Pageable<S>> {

  /**
   * Sorts by a column in ascending order.
   *
   * @param column the column name
   * @return a new builder with the sort key
   */
  S orderBy(String column);

  /**
   * Sorts by a column. Each call adds a sort key after the existing ones.
   *
   * @param column the column name
   * @param direction the sort direction
   * @return a new builder with the sort key
   */
  S orderBy(String column, Direction direction);

  /**
   * Limits the number of rows.
   *
   * @param rows the maximum number of rows, which must be greater than 0
   * @return a new builder with the limit
   * @throws IllegalArgumentException if {@code rows} is 0 or less
   */
  S limit(long rows);

  /**
   * Skips a number of rows.
   *
   * @param rows the number of rows to skip, which must be 0 or greater
   * @return a new builder with the offset
   * @throws IllegalArgumentException if {@code rows} is less than 0
   */
  S offset(long rows);
}
