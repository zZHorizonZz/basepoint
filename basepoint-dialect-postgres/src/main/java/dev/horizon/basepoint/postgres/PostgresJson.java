package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.JsonSyntax.Rows;
import dev.horizon.basepoint.StatementWriter;

/**
 * The JSON of PostgreSQL: {@code to_jsonb} of each row, whatever columns it has, and {@code jsonb_agg} of the rows in
 * the order of their numbers, which the objects then leave out.
 */
final class PostgresJson extends JsonSyntax {

  @Override
  protected void many(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    boolean numbered = rows.ordered();
    sql.append("(SELECT COALESCE(jsonb_agg(to_jsonb(" + alias + ")");
    if (numbered) {
      sql.append(" - '" + ROW_NUMBER + "' ORDER BY " + alias + ".").name(ROW_NUMBER);
    }
    sql.append("), '[]'::jsonb) FROM (");
    rows.write(sql, numbered, false);
    sql.append(") AS " + alias + ")");
  }

  @Override
  protected void one(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    sql.append("(SELECT to_jsonb(" + alias + ") FROM (");
    rows.write(sql, false, false);
    sql.append(") AS " + alias + ")");
  }
}
