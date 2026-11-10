package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/** {@code COUNT(*)}, the number of rows. */
public record CountAll() implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("COUNT(*)");
  }
}
