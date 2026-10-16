package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.StatementWriter;

/**
 * {@code LOCK IN SHARE MODE}, the MariaDB form of a share lock. MariaDB writes the other locks as the standard does.
 */
record MariaDbShareLock() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("LOCK IN SHARE MODE");
  }
}
