package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * A column by its name: a column of the SELECT list if the statement selects one of the name, as in
 * {@code ORDER BY "revenue"}, or a column of the source, as in {@code ORDER BY "t0"."total"}.
 *
 * @param name the column name
 */
public record Reference(String name) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.reference(name);
  }
}
