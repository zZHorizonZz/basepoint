package dev.horizon.basepoint.statement;

/**
 * {@code LIMIT}.
 *
 * @param rows the maximum number of rows, greater than 0
 */
public record Limit(long rows) implements Clause {

  public Limit {
    if (rows <= 0) {
      throw new IllegalArgumentException("The limit must be greater than 0, but is " + rows + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("LIMIT " + rows);
  }
}
