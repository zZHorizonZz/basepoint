package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * {@code BETWEEN}, inclusive.
 *
 * @param operand the value to test
 * @param low the lower bound
 * @param high the upper bound
 */
public record Between(Expression operand, Expression low, Expression high) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(operand).append(" BETWEEN ").render(low).append(" AND ").render(high).append(")");
  }
}
