package dev.horizon.basepoint;

/**
 * A builder of a statement.
 */
public interface Buildable {

  /**
   * Builds the statement, in the SQL of the dialect of the collection that started it.
   *
   * @return the SQL and its values
   */
  Statement build();
}
