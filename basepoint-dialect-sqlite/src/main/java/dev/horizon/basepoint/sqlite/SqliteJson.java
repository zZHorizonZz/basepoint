package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.JsonSyntax.Rows;
import dev.horizon.basepoint.StatementWriter;

/**
 * The JSON of SQLite: {@code json_group_array} of {@code json_object}, in the order of the numbers of the rows. JSON
 * that a subquery gives is text, which {@code json()} makes JSON again.
 */
final class SqliteJson extends JsonSyntax {

  @Override
  protected void many(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    boolean numbered = rows.ordered();
    sql.append("(SELECT COALESCE(json_group_array(");
    object(sql, "json_object", alias, rows);
    if (numbered) {
      sql.append(" ORDER BY " + alias + ".").name(ROW_NUMBER);
    }
    sql.append("), '[]') FROM (");
    rows.write(sql, numbered, false);
    sql.append(") AS " + alias + ")");
  }

  @Override
  protected void one(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    sql.append("(SELECT ");
    object(sql, "json_object", alias, rows);
    sql.append(" FROM (");
    rows.write(sql, false, false);
    sql.append(") AS " + alias + ")");
  }

  @Override
  protected String json(String value) {
    return "json(" + value + ")";
  }
}
