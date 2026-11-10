package dev.horizon.basepoint;

/**
 * Thrown when a statement uses a feature that the database of its dialect does not have, or has only within limits that
 * the statement exceeds.
 */
public final class UnsupportedFeatureException extends UnsupportedOperationException {

  /**
   * Creates the exception.
   *
   * @param message the feature, the database, and what to do instead
   */
  public UnsupportedFeatureException(String message) {
    super(message);
  }
}
