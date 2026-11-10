package dev.horizon.basepoint.statement;

/**
 * A clause of a {@link Statement}.
 *
 * <p>
 * The records in this interface render standard SQL. A dialect decorates the clauses that its database writes
 * differently, and can add clause types of its own.
 */
public interface Clause extends Node {

  /**
   * Names what the clause makes readable to the other clauses of its statement, before any of them renders. A join
   * names its relation here, so that a clause before the join can read the relation's columns.
   *
   * @param sql the writer that renders the statement
   */
  default void declare(StatementWriter sql) {
  }
}
