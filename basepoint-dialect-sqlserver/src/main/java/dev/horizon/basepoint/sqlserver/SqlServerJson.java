package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Include;
import dev.horizon.basepoint.StatementWriter;

/**
 * An include rendered as {@code FOR JSON PATH}.
 *
 * <p>
 * The include is wrapped in {@code JSON_QUERY} so that a nested include stays a JSON value and does not become a
 * string.
 *
 * @param include the include
 */
record SqlServerJson(Include include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    if (include.shape() == Include.Shape.ONE) {
      sql.append("JSON_QUERY((").render(include.rows().with(new ForJson(true))).append("))");
      return;
    }
    sql.append("JSON_QUERY(COALESCE((").render(include.rows().with(new ForJson(false))).append("), '[]'))");
  }

  /**
   * {@code FOR JSON PATH, INCLUDE_NULL_VALUES}.
   *
   * @param one {@code true} to add {@code WITHOUT_ARRAY_WRAPPER} and return a single object
   */
  record ForJson(boolean one) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("FOR JSON PATH, INCLUDE_NULL_VALUES" + (one ? ", WITHOUT_ARRAY_WRAPPER" : ""));
    }
  }
}
