package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/** The rows that a write of SQL Server gives: {@code OUTPUT INSERTED} or {@code OUTPUT DELETED}. */
final class SqlServerReturning extends ReturningSyntax {

  @Override
  public void head(StatementWriter sql, List<String> columns, boolean deleted) {
    String rows = deleted ? "DELETED." : "INSERTED.";
    sql.append(" OUTPUT ");
    if (columns.isEmpty()) {
      sql.append(rows + "*");
    }
    for (int i = 0; i < columns.size(); i++) {
      sql.append(i == 0 ? rows : ", " + rows).name(columns.get(i));
    }
  }

  @Override
  public void tail(StatementWriter sql, List<String> columns, boolean deleted) {
  }
}
