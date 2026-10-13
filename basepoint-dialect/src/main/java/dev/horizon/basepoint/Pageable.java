package dev.horizon.basepoint;

/**
 * The order of the rows of a read, and the part of them that it gives.
 *
 * @param <S> the type of the builder itself, which each step gives
 */
public interface Pageable<S extends Pageable<S>> {

  /**
   * Orders the rows by a column, ascending.
   *
   * @param column the name of the column
   * @return the builder with the order
   */
  S orderBy(String column);

  /**
   * Orders the rows by a column. Each order is for the rows that the orders before it leave equal.
   *
   * @param column the name of the column
   * @param direction the direction
   * @return the builder with the order
   */
  S orderBy(String column, Direction direction);

  /**
   * Gives at most a number of rows.
   *
   * @param rows the number, which must be positive
   * @return the builder with the limit
   */
  S limit(long rows);

  /**
   * Skips a number of rows.
   *
   * @param rows the number, which must not be negative
   * @return the builder that skips the rows
   */
  S offset(long rows);
}
