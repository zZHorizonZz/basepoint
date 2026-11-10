package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

record MariaDbObject(Include include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    Select select = include.rows().find(Select.class).orElseThrow();
    sql.append("(").render(include.rows().replace(select, new Select(false, List.of(new Pairs(include)))))
        .append(")");
  }

  record Pairs(Include include) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      List<Include.Output> outputs = include.outputs();
      sql.append("JSON_OBJECT(");
      for (int i = 0; i < outputs.size(); i++) {
        Include.Output output = outputs.get(i);
        sql.append(i == 0 ? "" : ", ").append("'" + output.name().replace("'", "''") + "', ").render(output.value());
      }
      sql.append(")");
    }
  }
}
