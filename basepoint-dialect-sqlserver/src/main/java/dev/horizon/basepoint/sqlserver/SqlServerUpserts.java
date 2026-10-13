package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpsertSyntax;
import dev.horizon.basepoint.UpsertSyntax.Upsert;
import java.util.List;

/**
 * The upsert of SQL Server: a {@code MERGE} of the rows as a table of values, which holds its lock on the key until the
 * end of the statement, so that two upserts of one key never both insert.
 */
final class SqlServerUpserts extends UpsertSyntax {

  @Override
  public void write(StatementWriter sql, Upsert insert) {
    String target = sql.alias();
    String excluded = sql.alias();
    List<String> columns = insert.columns();
    sql.append("MERGE INTO ").table(insert.table()).append(" WITH (HOLDLOCK) AS " + target + " USING (VALUES ");
    insert.writeValues(sql);
    sql.append(") AS " + excluded + " (");
    names(sql, columns);
    sql.append(") ON ");
    List<String> key = insert.key();
    for (int i = 0; i < key.size(); i++) {
      sql.append(i == 0 ? "" : " AND ").append(target + ".").name(key.get(i)).append(" = " + excluded + ".")
          .name(key.get(i));
    }
    List<String> updates = insert.updates();
    if (!updates.isEmpty()) {
      sql.append(" WHEN MATCHED THEN UPDATE SET ");
      for (int i = 0; i < updates.size(); i++) {
        sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = " + excluded + ".").name(updates.get(i));
      }
    }
    sql.append(" WHEN NOT MATCHED THEN INSERT (");
    names(sql, columns);
    sql.append(") VALUES (");
    for (int i = 0; i < columns.size(); i++) {
      sql.append(i == 0 ? "" : ", ").append(excluded + ".").name(columns.get(i));
    }
    sql.append(")");
    if (insert.returning() != null) {
      sql.dialect().returning().head(sql, insert.returning(), false);
    }
    sql.append(";");
  }
}
