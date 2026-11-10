package dev.horizon.basepoint;

import dev.horizon.basepoint.expression.Expression;
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
public final class CollectionReference {

  private final Dialect dialect;
  private final String table;

  CollectionReference(Dialect dialect, String table) {
    this.dialect = dialect;
    this.table = table;
  }

  /**
   * Returns the table name.
   *
   * @return the table name, for example {@code shop.orders}
   */
  public String name() {
    return table;
  }

  /**
   * Starts a query of all rows.
   *
   * @return the query
   */
  public Query query() {
    return new Query(dialect, table);
  }

  /**
   * Starts an insert of one row.
   *
   * @param row the column values. A value can be an {@link Expression} or {@code null}. An empty map inserts a
   * row with the default value of every column.
   * @return the insert
   */
  public InsertQuery insert(Map<String, ?> row) {
    return insert(List.of(row));
  }

  /**
   * Starts an insert of rows. Every row must set the same columns. The column order comes from the first row.
   *
   * @param rows the rows
   * @return the insert
   * @throws IllegalArgumentException if {@code rows} is empty or the rows set different columns
   */
  public InsertQuery insert(List<? extends Map<String, ?>> rows) {
    return new InsertQuery(dialect, table, rows);
  }

  /**
   * Starts an update. Add a filter with {@link UpdateQuery#where}, or call {@link UpdateQuery#all()} to update every
   * row.
   *
   * @return the update
   */
  public UpdateQuery update() {
    return new UpdateQuery(dialect, table);
  }

  /**
   * Starts a delete. Add a filter with {@link DeleteQuery#where}, or call {@link DeleteQuery#all()} to delete every
   * row.
   *
   * @return the delete
   */
  public DeleteQuery delete() {
    return new DeleteQuery(dialect, table);
  }
}
