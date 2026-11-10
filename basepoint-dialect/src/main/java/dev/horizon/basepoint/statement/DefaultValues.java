package dev.horizon.basepoint.statement;

/** {@code DEFAULT VALUES}: one row that uses the default value of every column. */
public record DefaultValues() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("DEFAULT VALUES");
  }
}
