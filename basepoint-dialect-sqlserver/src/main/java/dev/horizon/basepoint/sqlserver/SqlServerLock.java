package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.LockMode;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.From;
import dev.horizon.basepoint.statement.Lock;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.Map;
import java.util.function.UnaryOperator;

final class SqlServerLock implements UnaryOperator<Statement> {

  private static final Map<LockMode, String> HINTS = Map.of(LockMode.SHARE, "HOLDLOCK, ROWLOCK", LockMode.UPDATE,
      "UPDLOCK, ROWLOCK", LockMode.UPDATE_SKIP_LOCKED, "UPDLOCK, ROWLOCK, READPAST", LockMode.UPDATE_NOWAIT,
      "UPDLOCK, ROWLOCK, NOWAIT");

  @Override
  public Statement apply(Statement statement) {
    return statement.find(Lock.class).flatMap(lock -> statement.find(From.class).map(from -> statement
        .remove(lock).replace(from, new HintedFrom(HINTS.get(lock.mode()))))).orElse(statement);
  }

  record HintedFrom(String hints) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("FROM ").render(sql.source()).append(" AS " + sql.qualifier() + " WITH (" + hints + ")");
    }
  }
}
