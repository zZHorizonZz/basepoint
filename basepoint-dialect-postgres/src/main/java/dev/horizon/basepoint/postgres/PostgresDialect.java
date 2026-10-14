package dev.horizon.basepoint.postgres;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Include;

/**
 * The dialect for PostgreSQL 13 and later.
 *
 * <p>
 * Identifiers use double quotes. Placeholders default to {@code $1}, the style of the Vert.x PostgreSQL client. For
 * JDBC, use {@link ParameterStyle#QUESTION_MARK}. The dialect renders includes with {@code to_jsonb} and
 * {@code jsonb_agg}. All other clauses render as standard SQL.
 */
public final class PostgresDialect implements Dialect {

  public static final String NAME = "PostgreSQL";

  private final ParameterStyle parameters;
  private final Decorator decorator = Decorator.of(Include.class, PostgresJson::new);

  public PostgresDialect() {
    this(ParameterStyle.DOLLAR);
  }

  public PostgresDialect(ParameterStyle parameters) {
    this.parameters = parameters;
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
  public ParameterStyle parameters() {
    return parameters;
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }
}
