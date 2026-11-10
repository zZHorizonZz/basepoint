package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.StatementWriter;

record SqlServerArray(Include.Many include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("JSON_QUERY(COALESCE((").render(include.rows().with(new ForJson("FOR JSON PATH, INCLUDE_NULL_VALUES")))
        .append("), '[]'))");
  }

  record ForJson(String sql) implements Clause {

    @Override
    public void render(StatementWriter writer) {
      writer.append(sql);
    }
  }
}
