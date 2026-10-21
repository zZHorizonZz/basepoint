package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.StatementWriter;

/**
 * The value of a key of the JSON object in a column, rendered with {@code ->>}, which PostgreSQL has for {@code json}
 * and {@code jsonb} alike. The key is bound as text.
 *
 * @param key the key
 */
record PostgresJsonKey(Expression.JsonKey key) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").column(key.column()).append(" ->> CAST(").param(key.key()).append(" AS text))");
  }
}
