package dev.horizon.basepoint;

/**
 * An INSERT of one or more rows. Create an insert with {@link CollectionReference#insert}.
 *
 * <p>
 * By default, a row with the same key as an existing row makes the database reject the insert. Use {@link #ignore}
 * or {@link #replace} to handle such rows. Inserts are immutable. Each method returns a new insert.
 */
public interface InsertQuery extends Returnable<InsertQuery>, Buildable {

  /**
   * Skips each row whose key matches an existing row.
   *
   * @param key the columns of the primary key or unique index. MariaDB uses any unique key of the table.
   * @return a new insert that skips such rows
   * @throws IllegalArgumentException if {@code key} is empty or the insert sets no columns
   */
  InsertQuery ignore(String... key);

  /**
   * Updates the existing row when a row's key matches. The existing row takes all inserted columns except the key.
   *
   * @param key the columns of the primary key or unique index. MariaDB uses any unique key of the table.
   * @return a new insert that updates such rows
   * @throws IllegalArgumentException if {@code key} is empty or the insert sets no columns
   */
  InsertQuery replace(String... key);
}
