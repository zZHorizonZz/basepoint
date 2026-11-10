package dev.horizon.basepoint.sqlite;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.JsonKey;
import dev.horizon.basepoint.expression.Length;
import dev.horizon.basepoint.expression.Now;
import dev.horizon.basepoint.expression.Shift;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Lock;
import dev.horizon.basepoint.statement.Statement;

/**
 * The dialect for SQLite 3.44 and later.
 *
 * <p>
 * Identifiers use double quotes. Placeholders default to {@code ?}. The dialect renders includes with
 * {@code json_group_array}, the value of a JSON key with {@code json_extract}, and adds {@code LIMIT -1} to an offset
 * without a limit. SQLite has no locks of rows: a
 * transaction that writes locks the whole database, so the dialect writes no lock.
 *
 * <p>
 * SQLite keeps times as text. {@code now()} and a moved timestamp give UTC with six digits of fraction, such as
 * {@code 2026-01-05T10:00:00.123000Z}, so they compare as text with times that clients write to the microsecond. The
 * time functions of SQLite count to the millisecond, so the last three digits of a time that they give are 0.
 *
 * @param parameters the placeholder style
 */
public record SqliteDialect(ParameterStyle parameters) implements Dialect {

  public static final String NAME = "SQLite";

  private static final Decorator DECORATOR = Decorator.of(Statement.class, new SqliteLimits())
      .then(Decorator.of(Length.class, length -> (Expression) sql -> sql.append("length(")
          .render(length.text()).append(")")))
      .then(Decorator.of(Now.class, now -> (Expression) sql -> sql.append(
          "(strftime('%Y-%m-%dT%H:%M:%f', 'now') || '000Z')")))
      .then(Decorator.of(Shift.class, shift -> (Expression) sql -> sql.append(
          "(strftime('%Y-%m-%dT%H:%M:%f', ").render(shift.timestamp()).append(", '"
              + shift.seconds().toPlainString() + " seconds') || '000Z')")))
      .then(Decorator.of(Include.One.class, SqliteObject::new))
      .then(Decorator.of(Include.Many.class, SqliteArray::new))
      .then(Decorator.of(JsonKey.class, SqliteJsonKey::new))
      .then(Decorator.of(Statement.class, statement -> statement.find(Lock.class).map(statement::remove)
          .orElse(statement)));

  public SqliteDialect() {
    this(ParameterStyle.QUESTION_MARK);
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
