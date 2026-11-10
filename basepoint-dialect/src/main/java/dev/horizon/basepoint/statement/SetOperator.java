package dev.horizon.basepoint.statement;

/** A set operator between two {@link Operand} clauses. */
public enum SetOperator implements Clause {
  /** {@code UNION ALL}: the rows of both operands. */
  UNION_ALL("UNION ALL"),
  /** {@code UNION}: the rows of both operands, each once. */
  UNION("UNION"),
  /** {@code INTERSECT}: the rows of the left operand that the right operand also has. */
  INTERSECT("INTERSECT"),
  /** {@code EXCEPT}: the rows of the left operand that the right operand does not have. */
  EXCEPT("EXCEPT");

  private final String keyword;

  SetOperator(String keyword) {
    this.keyword = keyword;
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append(keyword);
  }
}
