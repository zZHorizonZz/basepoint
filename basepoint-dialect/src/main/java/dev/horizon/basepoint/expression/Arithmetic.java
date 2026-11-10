package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * An arithmetic operation of two numbers, such as {@code (left + right)}.
 *
 * @param left the first number
 * @param operator the operator
 * @param right the second number
 */
public record Arithmetic(Expression left, Operator operator, Expression right) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(left).append(" " + operator.symbol() + " ").render(right).append(")");
  }

  /** An arithmetic operator. */
  public enum Operator {
    /** {@code +}. */
    ADD("+"),
    /** {@code -}. */
    SUBTRACT("-"),
    /** {@code *}. */
    MULTIPLY("*"),
    /** {@code /}. */
    DIVIDE("/");

    private final String symbol;

    Operator(String symbol) {
      this.symbol = symbol;
    }

    /**
     * Returns the SQL of the operator.
     *
     * @return the symbol, for example {@code +}
     */
    public String symbol() {
      return symbol;
    }
  }
}
