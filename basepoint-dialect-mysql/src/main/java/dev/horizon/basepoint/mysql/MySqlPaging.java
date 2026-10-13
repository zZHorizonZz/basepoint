package dev.horizon.basepoint.mysql;

import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.StatementWriter;

/**
 * The limit of MySQL, which skips rows only after a limit: the greatest one, of an unsigned BIGINT, for all of them,
 * as the manual of MySQL gives it.
 */
final class MySqlPaging extends PagingSyntax {

  private static final String ALL_ROWS = Long.toUnsignedString(-1L);

  @Override
  public void paging(StatementWriter sql, Long limit, Long offset, boolean ordered) {
    if (limit == null && offset != null) {
      sql.append(" LIMIT " + ALL_ROWS + " OFFSET " + offset);
      return;
    }
    super.paging(sql, limit, offset, ordered);
  }
}
