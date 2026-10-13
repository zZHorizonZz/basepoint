package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.StatementWriter;

/**
 * The limit of SQL Server: {@code TOP} for a limit alone, and {@code OFFSET ... FETCH} to skip rows, which needs an
 * order.
 */
final class SqlServerPaging extends PagingSyntax {

  @Override
  public void top(StatementWriter sql, Long limit, Long offset) {
    if (limit != null && offset == null) {
      sql.append("TOP (" + limit + ") ");
    }
  }

  @Override
  public void paging(StatementWriter sql, Long limit, Long offset, boolean ordered) {
    if (offset == null) {
      return;
    }
    if (!ordered) {
      sql.append(" ORDER BY (SELECT NULL)");
    }
    sql.append(" OFFSET " + offset + " ROWS");
    if (limit != null) {
      sql.append(" FETCH NEXT " + limit + " ROWS ONLY");
    }
  }
}
