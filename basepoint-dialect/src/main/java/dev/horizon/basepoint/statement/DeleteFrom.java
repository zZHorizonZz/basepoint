package dev.horizon.basepoint.statement;

/** {@code DELETE FROM} with the statement's table. */
public record DeleteFrom() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("DELETE FROM " + sql.qualifier());
  }
}
