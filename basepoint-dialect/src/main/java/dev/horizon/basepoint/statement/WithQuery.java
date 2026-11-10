package dev.horizon.basepoint.statement;

/**
 * A query of a {@link With}: {@code name AS (rows)}.
 *
 * @param name the name, which the statement reads like a table
 * @param rows the statement of its rows
 * @param recursive true if the rows read the query itself, as {@code start UNION ALL step}
 */
public record WithQuery(String name, Statement rows, boolean recursive) implements Node {

  public WithQuery {
    rows.requireNestable();
  }

  @Override
  public void render(StatementWriter sql) {
    sql.name(name).append(" AS (").render(rows).append(")");
  }
}
