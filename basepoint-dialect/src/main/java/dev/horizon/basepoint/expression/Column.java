package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.Optional;

/**
 * A column of the current source, qualified with the source's alias or table name.
 *
 * @param name the column name
 */
public record Column(String name) implements Expression {

  @Override
  public Optional<String> label() {
    return Optional.of(name);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.column(name);
  }
}
