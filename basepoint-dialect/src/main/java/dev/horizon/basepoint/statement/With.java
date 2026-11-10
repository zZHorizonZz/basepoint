package dev.horizon.basepoint.statement;

import java.util.List;

/**
 * {@code WITH name AS (rows), ...}, the WITH queries of a statement.
 *
 * @param queries the WITH queries, in their order
 */
public record With(List<WithQuery> queries) implements Clause {

  public With {
    queries = List.copyOf(queries);
  }

  /**
   * Tells if one of the queries reads itself, which makes the whole list recursive.
   *
   * @return true if a query is recursive
   */
  public boolean recursive() {
    return queries.stream().anyMatch(WithQuery::recursive);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append(recursive() ? "WITH RECURSIVE " : "WITH ").render(queries, ", ");
  }
}
