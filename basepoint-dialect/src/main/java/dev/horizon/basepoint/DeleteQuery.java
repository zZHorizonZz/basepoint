package dev.horizon.basepoint;

/**
 * A delete of the rows of a table, from {@code delete()} of a collection.
 *
 * <p>
 * A filter chooses the rows. A delete needs {@link #where} or {@link #all}; without either, {@link #build} throws an
 * {@link IllegalStateException}, so that a forgotten filter never empties the whole table. Each step gives a new
 * delete.
 */
public interface DeleteQuery extends Filterable<DeleteQuery>, Returnable<DeleteQuery>, Buildable {

  /**
   * Deletes every row, without a filter.
   *
   * @return the delete of every row
   */
  DeleteQuery all();
}
