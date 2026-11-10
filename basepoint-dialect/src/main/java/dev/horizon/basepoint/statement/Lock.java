package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.LockMode;
import java.util.Map;
import java.util.Objects;

/**
 * The lock of the rows that a query reads: {@code FOR SHARE}, {@code FOR UPDATE}, {@code FOR UPDATE SKIP LOCKED}
 * or {@code FOR UPDATE NOWAIT}, at the end of the statement. A dialect whose database locks rows another way, or
 * not at all, decorates it into its own syntax.
 *
 * @param mode the lock
 */
public record Lock(LockMode mode) implements Clause {

  private static final Map<LockMode, String> SQL = Map.of(LockMode.SHARE, "FOR SHARE", LockMode.UPDATE, "FOR UPDATE",
      LockMode.UPDATE_SKIP_LOCKED, "FOR UPDATE SKIP LOCKED", LockMode.UPDATE_NOWAIT, "FOR UPDATE NOWAIT");

  public Lock {
    Objects.requireNonNull(mode, "mode");
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append(SQL.get(mode));
  }
}
