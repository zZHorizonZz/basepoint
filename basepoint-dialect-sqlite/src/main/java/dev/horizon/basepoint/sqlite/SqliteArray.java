package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record SqliteArray(Include.Many include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    sql.append("(SELECT COALESCE(json_group_array(");
    include.object(sql, "json_object", alias, value -> "json(" + value + ")");
    if (include.hasOrder()) {
      sql.append(" ORDER BY " + alias + ".").name(Include.ROW_NUMBER);
    }
    sql.append("), '[]') FROM (").render(include.numberRows()).append(") AS " + alias + ")");
  }
}
