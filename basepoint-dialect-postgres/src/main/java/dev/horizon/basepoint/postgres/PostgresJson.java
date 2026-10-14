package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Include;
import dev.horizon.basepoint.StatementWriter;

/**
 * An include rendered as {@code jsonb_agg} of {@code to_jsonb}.
 *
 * <p>
 * {@code to_jsonb} converts whole rows, so the included query does not need to list its columns. For sorted rows,
 * the aggregate orders by {@link Include#ROW_NUMBER} and removes that key from each object.
 *
 * @param include the include
 */
record PostgresJson(Include include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.alias();
    if (include.shape() == Include.Shape.ONE) {
      sql.append("(SELECT to_jsonb(" + alias + ") FROM (").render(include.rows()).append(") AS " + alias + ")");
      return;
    }
    sql.append("(SELECT COALESCE(jsonb_agg(to_jsonb(" + alias + ")");
    if (include.ordered()) {
      sql.append(" - '" + Include.ROW_NUMBER + "' ORDER BY " + alias + ".").name(Include.ROW_NUMBER);
    }
    sql.append("), '[]'::jsonb) FROM (").render(include.numbered()).append(") AS " + alias + ")");
  }
}
