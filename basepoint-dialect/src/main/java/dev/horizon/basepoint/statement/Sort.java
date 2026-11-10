package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.expression.Expression;

/**
 * A sort key of {@link OrderBy}.
 *
 * @param value the value to sort by
 * @param direction the sort direction
 */
public record Sort(Expression value, Direction direction) implements Node {

  @Override
  public void render(StatementWriter sql) {
    sql.render(value).append(direction == Direction.DESC ? " DESC" : " ASC");
  }
}
