package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * An integer literal, rendered in the SQL text.
 *
 * @param value the integer
 */
public record Literal(long value) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append(Long.toString(value));
  }
}
