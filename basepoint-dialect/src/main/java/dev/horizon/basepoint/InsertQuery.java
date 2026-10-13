package dev.horizon.basepoint;

/**
 * An insert of rows, from {@code insert()} of a collection.
 *
 * <p>
 * A row with the key of a row that is there fails the insert, unless {@link #ignore} or {@link #replace} says what to
 * do. Each step gives a new insert.
 */
public interface InsertQuery extends Returnable<InsertQuery>, Buildable {

  /**
   * Skips each row that has the values of a key that a row has already.
   *
   * @param key the columns of the primary key or of a unique index; MySQL takes any unique key
   * @return the insert that skips those rows
   */
  InsertQuery ignore(String... key);

  /**
   * Updates the row that has the values of a key with the other columns of the row that is not inserted.
   *
   * @param key the columns of the primary key or of a unique index; MySQL takes any unique key
   * @return the insert that replaces those rows
   */
  InsertQuery replace(String... key);
}
