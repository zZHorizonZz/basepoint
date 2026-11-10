package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record PostgresArray(Include.Many include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    sql.append("(SELECT COALESCE(jsonb_agg(to_jsonb(" + alias + ")");
    if (include.hasOrder()) {
      sql.append(" - '" + Include.ROW_NUMBER + "' ORDER BY " + alias + ".").name(Include.ROW_NUMBER);
    }
    sql.append("), '[]'::jsonb) FROM (").render(include.numberRows()).append(") AS " + alias + ")");
  }
}
