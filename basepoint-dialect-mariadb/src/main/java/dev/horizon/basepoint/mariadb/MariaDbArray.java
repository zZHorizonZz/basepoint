package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.OrderBy;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Sort;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;
import java.util.stream.Stream;

record MariaDbArray(Include.Many include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    Statement rows = new MariaDbLimits().apply(include.rows());
    List<Clause> paging = Stream.of(Limit.class, MariaDbLimits.AllRows.class, Offset.class)
        .flatMap(type -> rows.find(type).stream()).map(Clause.class::cast).toList();
    List<Clause> rest = rows.clauses().stream().filter(clause -> !paging.contains(clause)
        && !(clause instanceof OrderBy)).toList();
    Statement aggregate = new Statement(rows.source(), rest);
    Select select = aggregate.find(Select.class).orElseThrow();
    Expression array = new ArrayAgg(new MariaDbObject.Pairs(include), include.sorts(), paging);
    sql.append("COALESCE((").render(aggregate.replace(select, new Select(false, List.of(array))))
        .append("), JSON_ARRAY())");
  }

  record ArrayAgg(Expression value, List<Sort> sorts, List<Clause> paging) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("JSON_ARRAYAGG(").render(value);
      if (!sorts.isEmpty()) {
        sql.append(" ORDER BY ").render(sorts, ", ");
      }
      for (Clause clause : paging) {
        sql.append(" ").render(clause);
      }
      sql.append(")");
    }
  }
}
