package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.StatementWriter;

record MariaDbEmptyRow() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("() VALUES ()");
  }
}
