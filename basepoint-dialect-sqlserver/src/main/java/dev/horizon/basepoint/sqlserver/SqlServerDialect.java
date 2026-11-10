package dev.horizon.basepoint.sqlserver;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.expression.Concat;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Length;
import dev.horizon.basepoint.expression.Now;
import dev.horizon.basepoint.expression.Shift;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.With;

/**
 * The dialect for SQL Server 2017 and later.
 *
 * <p>
 * Identifiers use square brackets. Placeholders default to {@code @p1}, the style of the Vert.x SQL Server client.
 * For JDBC, use {@link ParameterStyle#QUESTION_MARK}. The dialect renders upserts as {@code MERGE}, returned rows as
 * {@code OUTPUT}, limits as {@code TOP} or {@code OFFSET ... FETCH}, includes as {@code FOR JSON}, and locks as
 * table hints, such as {@code WITH (UPDLOCK, ROWLOCK)}.
 *
 * @param parameters the placeholder style
 */
public record SqlServerDialect(ParameterStyle parameters) implements Dialect {

  public static final String NAME = "SQL Server";

  private static final Decorator DECORATOR = Decorator.of(Statement.class, new SqlServerLock())
      .then(Decorator.of(Concat.class, concat -> (Expression) sql -> sql.append("(")
          .render(concat.texts(), " + ").append(")")))
      .then(Decorator.of(Length.class, length -> (Expression) sql -> sql.append("(LEN(")
          .render(length.text()).append(" + 'x') - 1)")))
      .then(Decorator.of(Now.class, now -> (Expression) sql -> sql.append("SYSUTCDATETIME()")))
      .then(Decorator.of(Shift.class, SqlServerShift::new))
      .then(Decorator.of(With.class, with -> (Clause) sql -> sql.append("WITH ").render(with.queries(), ", ")))
      .then(Decorator.of(Statement.class, new SqlServerMerge()))
      .then(Decorator.of(Statement.class, new SqlServerOutput()))
      .then(Decorator.of(Statement.class, new SqlServerPaging()))
      .then(Decorator.of(Include.One.class, SqlServerObject::new))
      .then(Decorator.of(Include.Many.class, SqlServerArray::new));

  public SqlServerDialect() {
    this(ParameterStyle.AT_P);
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
  public Decorator decorator() {
    return DECORATOR;
  }
}
