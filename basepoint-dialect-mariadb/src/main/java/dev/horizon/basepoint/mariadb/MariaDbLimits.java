package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;

/**
 * Adds a limit to statements that have an offset but no limit. MariaDB requires a limit with an offset. The added
 * limit is the largest unsigned BIGINT, 18446744073709551615.
 */
final class MariaDbLimits implements Decorator {

  static final String ALL_ROWS = Long.toUnsignedString(-1L);

  @Override
  public Node decorate(Node node) {
    if (node instanceof Statement statement && statement.has(Clause.Offset.class)
        && !statement.has(Clause.Limit.class)) {
      return statement.before(Clause.Offset.class, new AllRows());
    }
    return node;
  }

  /** {@code LIMIT 18446744073709551615}. */
  record AllRows() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("LIMIT " + ALL_ROWS);
    }
  }
}
