package dev.horizon.basepoint.statement;

/** {@code UPDATE} with the statement's table. */
public record Update() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("UPDATE " + sql.qualifier());
  }
}
