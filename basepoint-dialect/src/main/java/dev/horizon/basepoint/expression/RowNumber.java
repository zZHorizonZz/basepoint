package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.Sort;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * {@code ROW_NUMBER() OVER (ORDER BY ...)}.
 *
 * @param sorts the sort keys of the window
 */
public record RowNumber(List<Sort> sorts) implements Expression {

  public RowNumber {
    sorts = List.copyOf(sorts);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("ROW_NUMBER() OVER (ORDER BY ").render(sorts, ", ").append(")");
  }
}
