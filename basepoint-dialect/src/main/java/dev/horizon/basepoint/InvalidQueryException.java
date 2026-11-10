package dev.horizon.basepoint;

/**
 * Thrown when queries cannot be put together as asked, such as a relation of grouped rows, a subquery that selects two
 * columns, or a union of queries from two dialects.
 */
public final class InvalidQueryException extends IllegalArgumentException {

  /**
   * Creates the exception.
   *
   * @param message the reason, and how to put the queries together instead
   */
  public InvalidQueryException(String message) {
    super(message);
  }
}
