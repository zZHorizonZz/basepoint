package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * The value of a key of the JSON object in a column. It renders as the standard {@code JSON_VALUE(column, ?)}, with
 * the path of the key bound. Dialects whose database has no {@code JSON_VALUE} decorate it.
 *
 * @param column the column name
 * @param key the key
 */
public record JsonKey(String column, String key) implements Expression {

  public JsonKey {
    if (key.isEmpty() || key.contains("\"") || key.contains("\\")) {
      throw new IllegalArgumentException("A JSON key must not be empty or contain \" or \\, but is " + key + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("JSON_VALUE(").column(column).append(", ").param(path()).append(")");
  }

  /**
   * Returns the path of the key, for example {@code $."env"}.
   *
   * @return the path
   */
  public String path() {
    return "$.\"" + key + "\"";
  }
}
