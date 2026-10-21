package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.StatementWriter;

/**
 * The value of a key of the JSON object in a column, rendered with {@code json_extract}, because SQLite has no
 * {@code JSON_VALUE}. {@code json_extract} gives a number as a number, so the value is cast to text, as the other
 * databases give it.
 *
 * @param key the key
 */
record SqliteJsonKey(Expression.JsonKey key) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("CAST(json_extract(").column(key.column()).append(", ").param(key.path()).append(") AS TEXT)");
  }
}
