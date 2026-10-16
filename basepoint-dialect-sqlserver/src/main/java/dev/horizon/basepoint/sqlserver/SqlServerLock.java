package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;

/**
 * Writes the lock of a query as table hints, the way SQL Server locks rows: {@code FROM ... WITH (UPDLOCK, ROWLOCK)}
 * in place of {@code FOR UPDATE} at the end.
 */
final class SqlServerLock implements Decorator {

  /**
   * {@code FROM} with the table hints of a lock.
   *
   * @param hints the hints, such as {@code UPDLOCK, ROWLOCK, READPAST}
   */
  record HintedFrom(String hints) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("FROM ").render(sql.source()).append(" AS " + sql.qualifier() + " WITH (" + hints + ")");
    }
  }

  @Override
  public Node decorate(Node node) {
    if (!(node instanceof Statement statement)) {
      return node;
    }
    return statement.find(Clause.Lock.class).flatMap(lock -> statement.find(Clause.From.class).map(from -> statement
        .remove(lock).replace(from, new HintedFrom(switch (lock.mode()) {
          case SHARE -> "HOLDLOCK, ROWLOCK";
          case UPDATE -> "UPDLOCK, ROWLOCK";
          case UPDATE_SKIP_LOCKED -> "UPDLOCK, ROWLOCK, READPAST";
          case UPDATE_NOWAIT -> "UPDLOCK, ROWLOCK, NOWAIT";
        })))).orElse(statement);
  }
}
