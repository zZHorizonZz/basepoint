package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.statement.StatementWriter;

/**
 * One of two values, by a condition: {@code CASE WHEN condition THEN then ELSE otherwise END}.
 *
 * @param condition the condition
 * @param then the value when the condition is true
 * @param otherwise the value when the condition is false or {@code NULL}
 */
public record Case(Filter condition, Expression then, Expression otherwise) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("CASE WHEN ").render(condition).append(" THEN ").render(then).append(" ELSE ").render(otherwise)
        .append(" END");
  }
}
