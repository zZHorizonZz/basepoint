package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.Objects;

/**
 * A bound value, rendered as a placeholder.
 *
 * @param value the value
 */
public record Value(Object value) implements Expression {

  public Value {
    Objects.requireNonNull(value, "value");
  }

  @Override
  public void render(StatementWriter sql) {
    sql.param(value);
  }
}
