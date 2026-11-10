package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.expression.Expression;

/**
 * A column assignment of {@link Assignments}: {@code column = value}.
 *
 * @param column the column name
 * @param value the new value
 */
public record Assignment(String column, Expression value) implements Node {

  @Override
  public void render(StatementWriter sql) {
    sql.name(column).append(" = ").render(value);
  }
}
