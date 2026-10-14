package dev.horizon.basepoint;

/**
 * An UPDATE of rows. Create an update with {@link CollectionReference#update()}.
 *
 * <p>
 * An update needs at least one assignment, and either a filter or {@link #all()}. Otherwise {@link #build()} throws
 * {@link IllegalStateException}. Updates are immutable. Each method returns a new update.
 */
public interface UpdateQuery
    extends Assignable<UpdateQuery>, Filterable<UpdateQuery>, Returnable<UpdateQuery>, Buildable {

  /**
   * Updates every row of the table.
   *
   * @return a new update without a filter
   */
  UpdateQuery all();
}
