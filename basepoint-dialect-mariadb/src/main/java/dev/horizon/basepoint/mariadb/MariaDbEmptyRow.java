package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.StatementWriter;

/** {@code () VALUES ()}: the MariaDB form of {@code DEFAULT VALUES}. */
record MariaDbEmptyRow() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("() VALUES ()");
  }
}
