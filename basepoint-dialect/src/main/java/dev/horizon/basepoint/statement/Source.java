package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.expression.Subquery;

/**
 * The rows that a statement reads or writes.
 *
 * <p>
 * A {@link Table} or {@link Rows} source gets a generated alias, such as {@code "t0"}, and its columns render as
 * {@code "t0"."id"}. SELECT statements read these. A {@link Target} uses the table name, and its columns render as
 * {@code "orders"."id"}. INSERT, UPDATE and DELETE statements write these. A statement with {@link None}, such as a
 * UNION, has no rows of its own.
 */
public sealed interface Source {

  /**
   * Renders the clauses of a statement with the columns of this source in scope.
   *
   * @param sql the writer that renders the statement
   * @param clauses renders the clauses
   */
  void enter(StatementWriter sql, Runnable clauses);

  /**
   * A table read under a generated alias.
   *
   * @param name the table name, for example {@code orders} or {@code shop.orders}
   */
  record Table(String name) implements Source, Node {

    @Override
    public void enter(StatementWriter sql, Runnable clauses) {
      sql.scope(sql.alias(), this, clauses);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.table(name);
    }
  }

  /**
   * The rows of a statement, read as a derived table under a generated alias.
   *
   * @param statement the statement whose rows are read
   */
  record Rows(Statement statement) implements Source {

    @Override
    public void enter(StatementWriter sql, Runnable clauses) {
      sql.scope(sql.alias(), new Subquery(statement), clauses);
    }
  }

  /**
   * A table that a statement writes, named as itself.
   *
   * @param name the table name, for example {@code orders} or {@code shop.orders}
   */
  record Target(String name) implements Source {

    @Override
    public void enter(StatementWriter sql, Runnable clauses) {
      sql.scope(sql.quoteTable(name), new Table(name), clauses);
    }
  }

  /** No rows, for a statement that combines the rows of other statements. */
  record None() implements Source {

    @Override
    public void enter(StatementWriter sql, Runnable clauses) {
      clauses.run();
    }
  }
}
