package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/** The {@code NULL} literal. */
public record Null() implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("NULL");
  }
}
