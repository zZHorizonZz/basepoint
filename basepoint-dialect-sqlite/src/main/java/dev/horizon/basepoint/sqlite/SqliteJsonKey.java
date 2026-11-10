package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.JsonKey;
import dev.horizon.basepoint.statement.StatementWriter;

record SqliteJsonKey(JsonKey key) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("CAST(json_extract(").column(key.column()).append(", ").param(key.path()).append(") AS TEXT)");
  }
}
