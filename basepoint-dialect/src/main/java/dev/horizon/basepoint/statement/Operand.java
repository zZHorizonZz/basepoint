package dev.horizon.basepoint.statement;

/**
 * A statement on one side of a {@link SetOperator}.
 *
 * @param statement the statement
 */
public record Operand(Statement statement) implements Clause {

  public Operand {
    statement.requireNestable();
  }

  @Override
  public void render(StatementWriter sql) {
    sql.render(statement);
  }
}
