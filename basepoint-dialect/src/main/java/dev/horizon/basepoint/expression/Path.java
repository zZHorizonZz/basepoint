package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * A column of a relation of the query, qualified with the alias of the relation.
 *
 * @param relation the name of the relation
 * @param name the column name
 */
public record Path(String relation, String name) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.path(relation, name);
  }
}
