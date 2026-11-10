package dev.horizon.basepoint.statement;

/** {@code FROM} with the statement's source and its alias. */
public record From() implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("FROM ").render(sql.source()).append(" AS " + sql.qualifier());
  }
}
