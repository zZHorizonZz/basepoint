package dev.horizon.basepoint;

import java.util.List;

/**
 * The insert that skips or updates the rows whose key is there: {@code ON CONFLICT} in standard SQL, with
 * {@code EXCLUDED} for the values of the row that is not inserted.
 */
public class UpsertSyntax {

  /** An insert with a key, as a dialect writes it. */
  public interface Upsert {

    /**
     * Gives the name of the table.
     *
     * @return the name
     */
    String table();

    /**
     * Gives the columns that the rows set, in order.
     *
     * @return the columns
     */
    List<String> columns();

    /**
     * Gives the columns of the key.
     *
     * @return the columns
     */
    List<String> key();

    /**
     * Gives the columns that the row that is there takes from the row that is not inserted.
     *
     * @return the columns; empty to skip the row
     */
    List<String> updates();

    /**
     * Gives the columns of the rows that the insert gives.
     *
     * @return the columns, empty for all, or null for none
     */
    List<String> returning();

    /**
     * Writes {@code INSERT INTO} the table, with the columns, the {@link ReturningSyntax#head} of the dialect and the
     * values.
     *
     * @param sql the writer of the statement
     */
    void writeInsert(StatementWriter sql);

    /**
     * Writes the values of the rows: {@code (?, ?), (?, ?)}.
     *
     * @param sql the writer of the statement
     */
    void writeValues(StatementWriter sql);
  }

  /**
   * Writes the upsert.
   *
   * @param sql the writer of the statement
   * @param upsert the insert with its key
   */
  public void write(StatementWriter sql, Upsert upsert) {
    upsert.writeInsert(sql);
    sql.append(" ON CONFLICT (");
    names(sql, upsert.key());
    sql.append(")");
    List<String> updates = upsert.updates();
    if (updates.isEmpty()) {
      sql.append(" DO NOTHING");
    } else {
      sql.append(" DO UPDATE SET ");
      for (int i = 0; i < updates.size(); i++) {
        sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = EXCLUDED.").name(updates.get(i));
      }
    }
    if (upsert.returning() != null) {
      sql.dialect().returning().tail(sql, upsert.returning(), false);
    }
  }

  /**
   * Writes names, separated by commas.
   *
   * @param sql the writer of the statement
   * @param names the names
   */
  protected void names(StatementWriter sql, List<String> names) {
    for (int i = 0; i < names.size(); i++) {
      sql.append(i == 0 ? "" : ", ").name(names.get(i));
    }
  }
}
