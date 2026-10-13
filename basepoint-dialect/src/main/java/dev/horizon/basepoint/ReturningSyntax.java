package dev.horizon.basepoint;

import java.util.List;

/**
 * The rows that an INSERT, an UPDATE or a DELETE gives. The methods write {@code RETURNING} at the end of the
 * statement.
 */
public class ReturningSyntax {

  /**
   * Writes the part that goes before the values of an INSERT, the WHERE of an UPDATE or the WHERE of a DELETE: nothing
   * in standard SQL.
   *
   * @param sql the writer of the statement
   * @param columns the columns, empty for all
   * @param deleted true for the rows of a DELETE
   */
  public void head(StatementWriter sql, List<String> columns, boolean deleted) {
  }

  /**
   * Writes the part that goes at the end of the statement.
   *
   * @param sql the writer of the statement
   * @param columns the columns, empty for all
   * @param deleted true for the rows of a DELETE
   */
  public void tail(StatementWriter sql, List<String> columns, boolean deleted) {
    sql.append(" RETURNING ");
    if (columns.isEmpty()) {
      sql.append("*");
    }
    for (int i = 0; i < columns.size(); i++) {
      sql.append(i == 0 ? "" : ", ").name(columns.get(i));
    }
  }
}
