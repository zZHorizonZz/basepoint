package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.filter.Filter;

/**
 * {@code WHERE}.
 *
 * @param filter the condition
 */
public record Where(Filter filter) implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("WHERE ").render(filter);
  }
}
