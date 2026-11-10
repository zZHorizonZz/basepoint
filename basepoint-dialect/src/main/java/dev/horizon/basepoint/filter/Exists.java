package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Literal;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * {@code EXISTS} of a statement.
 *
 * @param rows the statement
 */
public record Exists(Statement rows) implements Filter {

  public Exists {
    rows.requireNestable();
  }

  @Override
  public void render(StatementWriter sql) {
    Statement one = rows.find(Select.class).map(select -> rows.replace(select, new Select(false, List.of(
        new Literal(1))))).orElse(rows);
    sql.append("EXISTS (").render(one).append(")");
  }
}
