package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.expression.Expression;
import java.util.List;

/**
 * {@code VALUES} with one or more rows.
 *
 * @param rows the values of each row, in the order of the {@link InsertInto} columns
 */
public record Values(List<List<Expression>> rows) implements Clause {

  public Values {
    rows = rows.stream().map(List::copyOf).toList();
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("VALUES ");
    for (int i = 0; i < rows.size(); i++) {
      sql.append(i == 0 ? "(" : ", (").render(rows.get(i), ", ").append(")");
    }
  }
}
