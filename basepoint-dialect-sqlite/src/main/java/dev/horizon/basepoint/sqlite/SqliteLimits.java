package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.function.UnaryOperator;

final class SqliteLimits implements UnaryOperator<Statement> {

  @Override
  public Statement apply(Statement statement) {
    if (statement.has(Offset.class) && !statement.has(Limit.class)) {
      return statement.before(Offset.class, new AllRows());
    }
    return statement;
  }

  record AllRows() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("LIMIT -1");
    }
  }
}
