package dev.horizon.basepoint.sqlserver;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Include;

/**
 * The dialect for SQL Server 2017 and later.
 *
 * <p>
 * Identifiers use square brackets. Placeholders default to {@code @p1}, the style of the Vert.x SQL Server client.
 * For JDBC, use {@link ParameterStyle#QUESTION_MARK}. The dialect renders upserts as {@code MERGE}, returned rows as
 * {@code OUTPUT}, limits as {@code TOP} or {@code OFFSET ... FETCH}, and includes as {@code FOR JSON}.
 */
public final class SqlServerDialect implements Dialect {

  public static final String NAME = "SQL Server";

  private final ParameterStyle parameters;
  private final Decorator decorator = new SqlServerMerge().then(new SqlServerOutput()).then(new SqlServerPaging())
      .then(Decorator.of(Include.class, SqlServerJson::new));

  public SqlServerDialect() {
    this(ParameterStyle.AT_P);
  }

  public SqlServerDialect(ParameterStyle parameters) {
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String name) {
    return "[" + name.replace("]", "]]") + "]";
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
