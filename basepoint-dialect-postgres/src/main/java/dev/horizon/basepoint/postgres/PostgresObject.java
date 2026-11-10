package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record PostgresObject(Include.One include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    sql.append("(SELECT to_jsonb(" + alias + ") FROM (").render(include.rows()).append(") AS " + alias + ")");
  }
}
