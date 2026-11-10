package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * {@code NOT} of a filter.
 *
 * @param operand the filter
 */
public record Not(Filter operand) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(NOT ").render(operand).append(")");
  }
}
