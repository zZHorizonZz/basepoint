package dev.horizon.basepoint.statement;

import java.util.List;

/**
 * {@code ORDER BY}.
 *
 * @param sorts the sort keys, in priority order
 */
public record OrderBy(List<Sort> sorts) implements Clause {

  public OrderBy {
    sorts = List.copyOf(sorts);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("ORDER BY ").render(sorts, ", ");
  }
}
