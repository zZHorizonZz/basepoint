package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.JsonSyntax.Include;
import dev.horizon.basepoint.JsonSyntax.Rows;
import dev.horizon.basepoint.JsonSyntax.Shape;
import dev.horizon.basepoint.StatementWriter;

/**
 * The JSON of SQL Server: {@code FOR JSON PATH}, which keeps the order of the rows and their null columns. JSON in the
 * rows of {@code FOR JSON} is a string unless {@code JSON_QUERY} gives it, thus an include in the rows of an include
 * is given by it.
 */
final class SqlServerJson extends JsonSyntax {

  @Override
  public void include(StatementWriter sql, Include include, boolean json) {
    if (!json || include.shape() == Shape.COUNT) {
      super.include(sql, include, json);
      return;
    }
    sql.append("JSON_QUERY(");
    if (include.shape() == Shape.MANY) {
      many(sql, include.rows());
    } else {
      one(sql, include.rows());
    }
    sql.append(") AS ").name(include.name());
  }

  @Override
  protected void many(StatementWriter sql, Rows rows) {
    sql.append("COALESCE((");
    rows.write(sql, false, true);
    sql.append(" FOR JSON PATH, INCLUDE_NULL_VALUES), '[]')");
  }

  @Override
  protected void one(StatementWriter sql, Rows rows) {
    sql.append("(");
    rows.write(sql, false, true);
    sql.append(" FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER)");
  }
}
