package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.JsonKey;
import dev.horizon.basepoint.statement.StatementWriter;

record PostgresJsonKey(JsonKey key) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").column(key.column()).append(" ->> CAST(").param(key.key()).append(" AS text))");
  }
}
