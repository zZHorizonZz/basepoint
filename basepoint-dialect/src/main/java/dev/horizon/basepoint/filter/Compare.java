package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * A comparison: {@code (left operator right)}.
 *
 * @param left the left value
 * @param operator the operator
 * @param right the right value
 */
public record Compare(Expression left, Operator operator, Expression right) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(left).append(" " + operator.symbol + " ").render(right).append(")");
  }

  /** An operator of {@link Compare}. */
  public enum Operator {
    /** {@code =}. */
    EQ("="),
    /** {@code <>}. */
    NE("<>"),
    /** {@code >}. */
    GT(">"),
    /** {@code >=}. */
    GTE(">="),
    /** {@code <}. */
    LT("<"),
    /** {@code <=}. */
    LTE("<=");

    private final String symbol;

    Operator(String symbol) {
      this.symbol = symbol;
    }
  }
}
