package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/**
 * Rewrites an insert with {@link Clause.OnConflict} into a {@code MERGE} statement.
 *
 * <p>
 * The statement uses {@code WITH (HOLDLOCK)}, so concurrent upserts of the same key do not both insert. A
 * {@link Clause.Returning} clause becomes {@code OUTPUT INSERTED}.
 */
final class SqlServerMerge implements Decorator {

  @Override
  public Node decorate(Node node) {
    if (!(node instanceof Statement statement) || !statement.has(Clause.OnConflict.class)) {
      return node;
    }
    Merge merge = new Merge(statement.find(Clause.InsertInto.class).orElseThrow(),
        statement.find(Clause.Values.class).orElseThrow(), statement.find(Clause.OnConflict.class).orElseThrow(),
        statement.find(Clause.Returning.class).orElse(null));
    return new Statement(statement.source(), List.of(merge));
  }

  /**
   * A {@code MERGE} statement for an upsert.
   *
   * @param insert the inserted columns
   * @param values the inserted rows
   * @param conflict the key columns and the columns to update
   * @param returning the columns to return, or {@code null} to return nothing
   */
  record Merge(Clause.InsertInto insert, Clause.Values values, Clause.OnConflict conflict,
      Clause.Returning returning) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      String target = sql.alias();
      String excluded = sql.alias();
      List<String> columns = insert.columns();
      sql.append("MERGE INTO " + sql.qualifier() + " WITH (HOLDLOCK) AS " + target + " USING (").render(values)
          .append(") AS " + excluded + " (").names(columns).append(") ON ");
      List<String> key = conflict.key();
      for (int i = 0; i < key.size(); i++) {
        sql.append(i == 0 ? "" : " AND ").append(target + ".").name(key.get(i)).append(" = " + excluded + ".")
            .name(key.get(i));
      }
      List<String> updates = conflict.updates();
      if (!updates.isEmpty()) {
        sql.append(" WHEN MATCHED THEN UPDATE SET ");
        for (int i = 0; i < updates.size(); i++) {
          sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = " + excluded + ".").name(updates.get(i));
        }
      }
      sql.append(" WHEN NOT MATCHED THEN INSERT (").names(columns).append(") VALUES (");
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? "" : ", ").append(excluded + ".").name(columns.get(i));
      }
      sql.append(")");
      if (returning != null) {
        sql.append(" ").render(new SqlServerOutput.Output("INSERTED", returning.columns()));
      }
      sql.append(";");
    }
  }
}
