package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.Optional;

/**
 * A value with an alias: {@code value AS name}.
 *
 * @param value the value
 * @param name the alias
 */
public record As(Expression value, String name) implements Expression {

  @Override
  public Optional<String> label() {
    return Optional.of(name);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.render(value).append(" AS ").name(name);
  }
}
