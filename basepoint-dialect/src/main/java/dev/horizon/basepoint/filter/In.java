package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@code IN} or {@code NOT IN}. An empty list renders {@code (1 = 0)} for {@code IN} and {@code (1 = 1)} for
 * {@code NOT IN}.
 *
 * @param operand the value to test
 * @param values the values, each bound as a placeholder
 * @param negated {@code true} to render {@code NOT IN}
 */
public record In(Expression operand, List<?> values, boolean negated) implements Filter {

  public In {
    values = Collections.unmodifiableList(new ArrayList<>(values));
  }

  @Override
  public void render(StatementWriter sql) {
    if (values.isEmpty()) {
      sql.append(negated ? "(1 = 1)" : "(1 = 0)");
      return;
    }
    List<?> present = values.stream().filter(Objects::nonNull).toList();
    boolean absent = present.size() < values.size();
    if (present.isEmpty()) {
      sql.append("(").render(operand).append(negated ? " IS NOT NULL)" : " IS NULL)");
      return;
    }
    sql.append(absent && !negated ? "((" : "(").render(operand).append(negated ? " NOT IN (" : " IN (");
    for (int i = 0; i < present.size(); i++) {
      sql.append(i == 0 ? "" : ", ").render(Expression.value(present.get(i)));
    }
    sql.append("))");
    if (absent && !negated) {
      sql.append(" OR (").render(operand).append(" IS NULL))");
    }
  }
}
