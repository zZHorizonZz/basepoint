package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/** All columns of the current source, for example {@code "t0".*}. */
public record AllColumns() implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append(sql.qualifier() + ".*");
  }
}
