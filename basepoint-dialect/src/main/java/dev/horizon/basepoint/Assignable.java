package dev.horizon.basepoint;

/**
 * The values of an update.
 *
 * @param <S> the type of the builder itself, which each step gives
 */
public interface Assignable<S extends Assignable<S>> {

  /**
   * Sets a column to a value.
   *
   * @param column the name of the column
   * @param value the value, an {@link Expression}, or null
   * @return the builder with the value
   */
  S set(String column, Object value);

  /**
   * Adds a number to a column.
   *
   * @param column the name of the column
   * @param amount the number, which can be negative
   * @return the builder with the value
   */
  S increment(String column, Number amount);

  /**
   * Sets a column to null.
   *
   * @param column the name of the column
   * @return the builder with the value
   */
  S unset(String column);
}
