package dev.horizon.basepoint;

/**
 * The column assignments of an update. A later assignment to the same column replaces the earlier one.
 *
 * @param <S> the builder type that each method returns
 */
public interface Assignable<S extends Assignable<S>> {

  /**
   * Sets a column to a value.
   *
   * @param column the column name
   * @param value the value, an {@link Expression}, or {@code null}
   * @return a new builder with the assignment
   */
  S set(String column, Object value);

  /**
   * Adds a number to a column: {@code column = (column + amount)}.
   *
   * @param column the column name
   * @param amount the number to add, which can be negative
   * @return a new builder with the assignment
   */
  S increment(String column, Number amount);

  /**
   * Sets a column to {@code NULL}.
   *
   * @param column the column name
   * @return a new builder with the assignment
   */
  S unset(String column);
}
