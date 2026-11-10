package dev.horizon.basepoint;

/**
 * Thrown when an UPDATE or a DELETE has no filter, so it would write every row, and does not say so with
 * {@code all()}.
 */
public final class UnfilteredWriteException extends IllegalStateException {

  /**
   * Creates the exception.
   *
   * @param message the reason, and how to say that every row is meant
   */
  public UnfilteredWriteException(String message) {
    super(message);
  }
}
