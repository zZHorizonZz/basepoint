package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * An aggregate function, such as {@code SUM(total)}.
 *
 * @param function the function
 * @param argument the aggregated value
 */
public record Aggregate(Function function, Expression argument) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append(function.name() + "(").render(argument).append(")");
  }

  /** A function of {@link Aggregate}, named as SQL names it. */
  public enum Function {
    /** The number of rows where the value is not {@code NULL}. */
    COUNT,
    /** The sum of the values. */
    SUM,
    /** The average of the values. */
    AVG,
    /** The smallest value. */
    MIN,
    /** The largest value. */
    MAX
  }
}
