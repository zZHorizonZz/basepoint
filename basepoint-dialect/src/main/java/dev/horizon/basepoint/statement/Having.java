package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.filter.Filter;

/**
 * {@code HAVING}.
 *
 * @param filter the condition on each group
 */
public record Having(Filter filter) implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("HAVING ").render(filter);
  }
}
