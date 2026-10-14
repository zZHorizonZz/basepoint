package dev.horizon.basepoint;

import java.util.List;
import java.util.Map;

/**
 * A reference to one table. Create a reference with {@link Dialect#collection(String)}.
 *
 * <p>
 * Each method starts a builder: {@link #query()} for reads, {@link #insert} for inserts, {@link #update()} for
 * updates and {@link #delete()} for deletes. Call {@code build()} on a builder to get SQL text and values for any
 * database client.
 */
public interface CollectionReference {

  /**
   * Returns the table name.
   *
   * @return the table name, for example {@code shop.orders}
   */
  String name();

  /**
   * Starts a query of all rows.
   *
   * @return the query
   */
  Query query();

  /**
   * Starts an insert of one row.
   *
   * @param row the column values. A value can be an {@link Expression} or {@code null}. An empty map inserts a
   * row with the default value of every column.
   * @return the insert
   */
  InsertQuery insert(Map<String, ?> row);

  /**
   * Starts an insert of rows. Every row must set the same columns. The column order comes from the first row.
   *
   * @param rows the rows
   * @return the insert
   * @throws IllegalArgumentException if {@code rows} is empty or the rows set different columns
   */
  InsertQuery insert(List<? extends Map<String, ?>> rows);

  /**
   * Starts an update. Add a filter with {@link UpdateQuery#where}, or call {@link UpdateQuery#all()} to update every
   * row.
   *
   * @return the update
   */
  UpdateQuery update();

  /**
   * Starts a delete. Add a filter with {@link DeleteQuery#where}, or call {@link DeleteQuery#all()} to delete every
   * row.
   *
   * @return the delete
   */
  DeleteQuery delete();
}
