package dev.horizon.basepoint.sqlite;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Include;

/**
 * The dialect for SQLite 3.44 and later.
 *
 * <p>
 * Identifiers use double quotes. Placeholders default to {@code ?}. The dialect renders includes with
 * {@code json_group_array} and adds {@code LIMIT -1} to an offset without a limit.
 */
public final class SqliteDialect implements Dialect {

  public static final String NAME = "SQLite";

  private final ParameterStyle parameters;
  private final Decorator decorator = new SqliteLimits().then(Decorator.of(Include.class, SqliteJson::new));

  public SqliteDialect() {
    this(ParameterStyle.QUESTION_MARK);
  }

  public SqliteDialect(ParameterStyle parameters) {
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
