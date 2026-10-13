package dev.horizon.basepoint.mysql;

import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/** MySQL, which has no form of the rows that a write gives. */
final class MySqlReturning extends ReturningSyntax {

  @Override
  public void head(StatementWriter sql, List<String> columns, boolean deleted) {
    throw new UnsupportedOperationException(
        "MySQL has no RETURNING; read the rows with a SELECT in the transaction of the write");
  }

  @Override
  public void tail(StatementWriter sql, List<String> columns, boolean deleted) {
    head(sql, columns, deleted);
  }
}
