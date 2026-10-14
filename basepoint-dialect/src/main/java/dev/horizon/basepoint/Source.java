package dev.horizon.basepoint;

/**
 * The rows that a statement reads or writes: a table or a subquery.
 *
 * <p>
 * An aliased source gets a generated alias, such as {@code "t0"}, and its columns render as {@code "t0"."id"}. A
 * named source uses the table name, and its columns render as {@code "orders"."id"}. SELECT statements use aliased
 * sources. INSERT, UPDATE and DELETE statements use named sources.
 *
 * @param rows the table or subquery
 * @param aliased {@code true} to use a generated alias, {@code false} to use the table name
 */
public record Source(Node rows, boolean aliased) {

  /**
   * Returns an aliased source for a table.
   *
   * @param table the table name
   * @return the source
   */
  public static Source aliased(String table) {
    return new Source(new Table(table), true);
  }

  /**
   * Returns a named source for a table.
   *
   * @param table the table name
   * @return the source
   */
  public static Source named(String table) {
    return new Source(new Table(table), false);
  }

  /**
   * Returns an aliased source for the rows of a statement.
   *
   * @param statement the statement to read from
   * @return the source
   */
  public static Source of(Statement statement) {
    return new Source(new Expression.Subquery(statement), true);
  }

  /**
   * Returns the SQL that qualifies the columns of this source.
   *
   * @param sql the writer that renders the statement
   * @return a new alias for an aliased source, or the quoted table name for a named source
   * @throws IllegalStateException if the source is named but is not a table
   */
  public String qualifier(StatementWriter sql) {
    if (aliased) {
      return sql.alias();
    }
    if (!(rows instanceof Table table)) {
      throw new IllegalStateException("A named source must be a table.");
    }
    return sql.quoteTable(table.name());
  }

  /**
   * A table.
   *
   * @param name the table name, for example {@code orders} or {@code shop.orders}
   */
  public record Table(String name) implements Node {

    @Override
    public void render(StatementWriter sql) {
      sql.table(name);
    }
  }
}
