package dev.horizon.basepoint;

import java.util.List;
import java.util.Map;

/**
 * The rows of one table, in the SQL of a dialect.
 *
 * <p>
 * Each verb starts its own builder: {@link #query} reads, {@link #insert} adds, {@link #update} changes and
 * {@link #delete} deletes. A builder gives a {@link Statement}, which any client runs.
 */
public interface CollectionReference {

  /**
   * Gives the name of the table.
   *
   * @return the name, whose parts, such as its schema, are separated by dots
   */
  String name();

  /**
   * Starts a read of all rows.
   *
   * @return the query
   */
  Query query();

  /**
   * Starts an insert of a row.
   *
   * @param row the values of its columns, each a value, an {@link Expression} or null; with none, the defaults of
   * every column
   * @return the insert
   */
  InsertQuery insert(Map<String, ?> row);

  /**
   * Starts an insert of rows, which all set the same columns.
   *
   * @param rows the rows
   * @return the insert
   */
  InsertQuery insert(List<? extends Map<String, ?>> rows);

  /**
   * Starts an update of rows. Narrow it with {@link UpdateQuery#where}, or update every row with
   * {@link UpdateQuery#all}.
   *
   * @return the update
   */
  UpdateQuery update();

  /**
   * Starts a delete of rows. Narrow it with {@link DeleteQuery#where}, or delete every row with
   * {@link DeleteQuery#all}.
   *
   * @return the delete
   */
  DeleteQuery delete();
}
