package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;

/** Adds {@code LIMIT -1} to statements that have an offset but no limit. SQLite requires a limit with an offset. */
final class SqliteLimits implements Decorator {

  @Override
  public Node decorate(Node node) {
    if (node instanceof Statement statement && statement.has(Clause.Offset.class)
        && !statement.has(Clause.Limit.class)) {
      return statement.before(Clause.Offset.class, new Clause.Limit(-1));
    }
    return node;
  }
}
