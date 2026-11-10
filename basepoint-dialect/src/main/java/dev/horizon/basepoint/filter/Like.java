package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * {@code LIKE} with a pattern as it is.
 *
 * @param operand the string to test
 * @param pattern the pattern, with the wildcards of the database
 */
public record Like(Expression operand, String pattern) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(operand).append(" LIKE ").param(pattern).append(")");
  }
}
