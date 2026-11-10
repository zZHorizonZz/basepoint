package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.expression.Expression;
import java.util.List;

/**
 * {@code GROUP BY}.
 *
 * @param columns the grouping values
 */
public record GroupBy(List<Expression> columns) implements Clause {

  public GroupBy {
    columns = List.copyOf(columns);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("GROUP BY ").render(columns, ", ");
  }
}
