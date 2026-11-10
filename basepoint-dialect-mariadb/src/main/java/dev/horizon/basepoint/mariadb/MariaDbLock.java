package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.LockMode;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Lock;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.Map;

record MariaDbLock(Lock lock) implements Clause {

  private static final Map<LockMode, String> SQL = Map.of(LockMode.SHARE, "LOCK IN SHARE MODE", LockMode.UPDATE,
      "FOR UPDATE", LockMode.UPDATE_SKIP_LOCKED, "FOR UPDATE SKIP LOCKED", LockMode.UPDATE_NOWAIT, "FOR UPDATE NOWAIT");

  @Override
  public void render(StatementWriter sql) {
    sql.append(SQL.get(lock.mode()));
  }
}
