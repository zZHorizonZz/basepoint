package dev.horizon.basepoint;

/** A builder that produces a statement. */
public interface Buildable {

  /**
   * Returns the statement tree, before any decorator rewrites it.
   *
   * @return the statement
   */
  Statement statement();

  /**
   * Renders the statement in the dialect of the collection that created this builder.
   *
   * @return the SQL text and its placeholder values
   */
  RenderedStatement build();
}
