package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/**
 * Rewrites {@link Clause.Returning} into {@code OUTPUT}.
 *
 * <p>
 * The clause moves to the position that SQL Server expects: after the column list of an INSERT, after the
 * {@code SET} of an UPDATE, or after the table of a DELETE. Deletes return {@code DELETED} rows. Inserts and updates
 * return {@code INSERTED} rows.
 */
final class SqlServerOutput implements Decorator {

  @Override
  public Node decorate(Node node) {
    if (!(node instanceof Statement statement)) {
      return node;
    }
    return statement.find(Clause.Returning.class).map(returning -> {
      boolean deleted = statement.has(Clause.DeleteFrom.class);
      Output output = new Output(deleted ? "DELETED" : "INSERTED", returning.columns());
      Statement without = statement.remove(returning);
      if (without.has(Clause.Set.class)) {
        return without.after(Clause.Set.class, output);
      }
      return deleted ? without.after(Clause.DeleteFrom.class, output) : without.after(Clause.InsertInto.class, output);
    }).orElse(statement);
  }

  /**
   * {@code OUTPUT INSERTED.*} or {@code OUTPUT DELETED.*}, or the same with a column list.
   *
   * @param rows {@code INSERTED} or {@code DELETED}
   * @param columns the columns to return. An empty list returns all columns.
   */
  record Output(String rows, List<String> columns) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("OUTPUT ");
      if (columns.isEmpty()) {
        sql.append(rows + ".*");
      }
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? rows + "." : ", " + rows + ".").name(columns.get(i));
      }
    }
  }
}
