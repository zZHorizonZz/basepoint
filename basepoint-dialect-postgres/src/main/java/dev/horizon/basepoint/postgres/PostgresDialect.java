package dev.horizon.basepoint.postgres;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.expression.JsonKey;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.Include;

/**
 * The dialect for PostgreSQL 13 and later.
 *
 * <p>
 * Identifiers use double quotes. Placeholders default to {@code $1}, the style of the Vert.x PostgreSQL client. For
 * JDBC, use {@link ParameterStyle#QUESTION_MARK}. The dialect renders includes with {@code to_jsonb} and
 * {@code jsonb_agg}, and the value of a JSON key with {@code ->>}. All other clauses render as standard SQL.
 *
 * @param parameters the placeholder style
 */
public record PostgresDialect(ParameterStyle parameters) implements Dialect {

  public static final String NAME = "PostgreSQL";

  private static final Decorator DECORATOR = Decorator.of(Include.One.class, PostgresObject::new)
      .then(Decorator.of(Include.Many.class, PostgresArray::new))
      .then(Decorator.of(JsonKey.class, PostgresJsonKey::new));

  public PostgresDialect() {
    this(ParameterStyle.DOLLAR);
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String name) {
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  @Override
  public Decorator decorator() {
    return DECORATOR;
  }
}
