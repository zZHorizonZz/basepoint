package dev.horizon.basepoint;

/**
 * An update of the rows of a table, from {@code update()} of a collection.
 *
 * <p>
 * A filter chooses the rows. An update needs {@link #where} or {@link #all}; without either, {@link #build} throws an
 * {@link IllegalStateException}, so that a forgotten filter never changes the whole table. Each step gives a new
 * update.
 */
public interface UpdateQuery
    extends Assignable<UpdateQuery>, Filterable<UpdateQuery>, Returnable<UpdateQuery>, Buildable {

  /**
   * Updates every row, without a filter.
   *
   * @return the update of every row
   */
  UpdateQuery all();
}
