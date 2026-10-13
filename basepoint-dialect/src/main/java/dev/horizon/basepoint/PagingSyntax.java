package dev.horizon.basepoint;

/**
 * The limit of the rows of a SELECT, and the rows that it skips. The methods write {@code LIMIT} and {@code OFFSET}
 * after the order.
 */
public class PagingSyntax {

  /**
   * Writes the part of the limit that goes after {@code SELECT} and {@code DISTINCT}: nothing in standard SQL.
   *
   * @param sql the writer of the statement
   * @param limit the number of rows, or null for all
   * @param offset the number of rows to skip, or null for none
   */
  public void top(StatementWriter sql, Long limit, Long offset) {
  }

  /**
   * Writes the part of the limit that goes after the order.
   *
   * @param sql the writer of the statement
   * @param limit the number of rows, or null for all
   * @param offset the number of rows to skip, or null for none
   * @param ordered true if the SELECT has an order
   */
  public void paging(StatementWriter sql, Long limit, Long offset, boolean ordered) {
    if (limit != null) {
      sql.append(" LIMIT " + limit);
    }
    if (offset != null) {
      sql.append(" OFFSET " + offset);
    }
  }
}
