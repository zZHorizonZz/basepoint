package dev.horizon.basepoint.statement;

/**
 * {@code OFFSET}.
 *
 * @param rows the number of rows to skip, 0 or greater
 */
public record Offset(long rows) implements Clause {

  public Offset {
    if (rows < 0) {
      throw new IllegalArgumentException("The offset must be 0 or greater, but is " + rows + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("OFFSET " + rows);
  }
}
