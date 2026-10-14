package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Include;
import dev.horizon.basepoint.StatementWriter;

/**
 * An include rendered as {@code json_group_array} of {@code json_object}.
 *
 * <p>
 * A subquery returns JSON as text, so nested includes are wrapped in {@code json()} to keep them as JSON values.
 *
 * @param include the include
 */
record SqliteJson(Include include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    if (include.shape() == Include.Shape.ONE) {
      sql.append("(SELECT ");
      include.object(sql, "json_object", alias, value -> "json(" + value + ")");
      sql.append(" FROM (").render(include.rows()).append(") AS " + alias + ")");
      return;
    }
    sql.append("(SELECT COALESCE(json_group_array(");
    include.object(sql, "json_object", alias, value -> "json(" + value + ")");
    if (include.ordered()) {
      sql.append(" ORDER BY " + alias + ".").name(Include.ROW_NUMBER);
    }
    sql.append("), '[]') FROM (").render(include.numbered()).append(") AS " + alias + ")");
  }
}
