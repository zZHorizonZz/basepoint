package dev.horizon.basepoint.mysql;

import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.JsonSyntax.Rows;
import dev.horizon.basepoint.StatementWriter;

/**
 * The JSON of MySQL: {@code JSON_ARRAYAGG} of {@code JSON_OBJECT}. The aggregate has no order of its own, thus rows
 * with an order are aggregated over a window in the order of their numbers, whose first row has them all.
 */
final class MySqlJson extends JsonSyntax {

  @Override
  protected void many(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    boolean numbered = rows.ordered();
    sql.append("COALESCE((SELECT JSON_ARRAYAGG(");
    object(sql, "JSON_OBJECT", alias, rows);
    sql.append(")");
    if (numbered) {
      sql.append(" OVER (ORDER BY " + alias + ".").name(ROW_NUMBER)
          .append(" ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)");
    }
    sql.append(" FROM (");
    rows.write(sql, numbered, false);
    sql.append(") AS " + alias + (numbered ? " LIMIT 1" : "") + "), JSON_ARRAY())");
  }

  @Override
  protected void one(StatementWriter sql, Rows rows) {
    String alias = sql.alias();
    sql.append("(SELECT ");
    object(sql, "JSON_OBJECT", alias, rows);
    sql.append(" FROM (");
    rows.write(sql, false, false);
    sql.append(") AS " + alias + ")");
  }

  @Override
  protected String json(String value) {
    return "CAST(" + value + " AS JSON)";
  }
}
