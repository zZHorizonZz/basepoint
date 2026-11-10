package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * The current time in UTC, rendered as the standard {@code CURRENT_TIMESTAMP}. Dialects whose database gives
 * another time or form decorate it.
 */
public record Now() implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("CURRENT_TIMESTAMP");
  }
}
