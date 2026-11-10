package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record SqliteObject(Include.One include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    sql.append("(SELECT ");
    include.object(sql, "json_object", alias, value -> "json(" + value + ")");
    sql.append(" FROM (").render(include.rows()).append(") AS " + alias + ")");
  }
}
