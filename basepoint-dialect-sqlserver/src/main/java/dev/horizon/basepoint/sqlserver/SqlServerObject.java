package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record SqlServerObject(Include.One include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("JSON_QUERY((").render(include.rows().with(new SqlServerArray.ForJson(
        "FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER"))).append("))");
  }
}
