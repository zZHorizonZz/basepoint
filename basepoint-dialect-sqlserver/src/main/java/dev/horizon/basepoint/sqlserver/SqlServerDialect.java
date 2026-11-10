package dev.horizon.basepoint.sqlserver;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Include;

/**
 * The dialect for SQL Server 2017 and later.
 *
 * <p>
 * Identifiers use square brackets. Placeholders default to {@code @p1}, the style of the Vert.x SQL Server client.
 * For JDBC, use {@link ParameterStyle#QUESTION_MARK}. The dialect renders upserts as {@code MERGE}, returned rows as
 * {@code OUTPUT}, limits as {@code TOP} or {@code OFFSET ... FETCH}, includes as {@code FOR JSON}, and locks as
 * table hints, such as {@code WITH (UPDLOCK, ROWLOCK)}.
 */
public final class SqlServerDialect implements Dialect {

  public static final String NAME = "SQL Server";

  private final ParameterStyle parameters;
  private final Decorator decorator = new SqlServerLock()
      .then(Decorator.of(Expression.Concat.class, concat -> (Expression) sql -> sql.append("(")
          .render(concat.texts(), " + ").append(")")))
      .then(Decorator.of(Expression.Length.class, length -> (Expression) sql -> sql.append("(LEN(")
          .render(length.text()).append(" + 'x') - 1)")))
      .then(Decorator.of(Expression.Now.class, now -> (Expression) sql -> sql.append("SYSUTCDATETIME()")))
      .then(Decorator.of(Expression.Shift.class, SqlServerShift::new))
      .then(new SqlServerMerge()).then(new SqlServerOutput())
      .then(new SqlServerPaging())
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
