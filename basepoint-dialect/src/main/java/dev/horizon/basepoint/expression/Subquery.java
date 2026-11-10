package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * A statement in parentheses, used as a value or as a source.
 *
 * @param statement the statement
 */
public record Subquery(Statement statement) implements Expression {

  public Subquery {
    statement.requireNestable();
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(statement).append(")");
  }
}
