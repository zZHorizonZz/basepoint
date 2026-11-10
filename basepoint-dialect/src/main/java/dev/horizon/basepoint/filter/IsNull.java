package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * {@code IS NULL} or {@code IS NOT NULL}.
 *
 * @param operand the value to test
 * @param negated {@code true} to render {@code IS NOT NULL}
 */
public record IsNull(Expression operand, boolean negated) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(operand).append(negated ? " IS NOT NULL)" : " IS NULL)");
  }
}
