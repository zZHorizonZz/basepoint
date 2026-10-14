package dev.horizon.basepoint;

/**
 * A DELETE of rows. Create a delete with {@link CollectionReference#delete()}.
 *
 * <p>
 * A delete needs a filter or {@link #all()}. Otherwise {@link #build()} throws {@link IllegalStateException}. Deletes
 * are immutable. Each method returns a new delete.
 */
public interface DeleteQuery extends Filterable<DeleteQuery>, Returnable<DeleteQuery>, Buildable {

  /**
   * Deletes every row of the table.
   *
   * @return a new delete without a filter
   */
  DeleteQuery all();
}
