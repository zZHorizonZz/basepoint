package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.StatementWriter;

/** The limit of SQLite, which skips rows only after a limit: {@code LIMIT -1} for all of them. */
final class SqlitePaging extends PagingSyntax {

  @Override
  public void paging(StatementWriter sql, Long limit, Long offset, boolean ordered) {
    super.paging(sql, limit == null && offset != null ? -1L : limit, offset, ordered);
  }
}
