package dev.horizon.basepoint.mariadb;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.expression.Concat;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Now;
import dev.horizon.basepoint.expression.Shift;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.DefaultValues;
import dev.horizon.basepoint.statement.Include;
import dev.horizon.basepoint.statement.Lock;
import dev.horizon.basepoint.statement.OnConflict;
import dev.horizon.basepoint.statement.Statement;

/**
 * The dialect for MariaDB 11.4 and later.
 *
 * <p>
 * Identifiers use backticks. Placeholders default to {@code ?}. The dialect renders upserts as
 * {@code ON DUPLICATE KEY UPDATE} and includes as {@code JSON_ARRAYAGG}. It renders {@code RETURNING} for inserts,
 * updates and deletes. MariaDB supports {@code UPDATE ... RETURNING} from version 13.0. A share lock is
 * {@code LOCK IN SHARE MODE}.
 *
 * <p>
 * {@code JSON_ARRAYAGG} returns at most {@code group_concat_max_len} bytes, which is 1 MB by default. Raise that
 * setting if an include can return more.
 *
 * @param parameters the placeholder style
 */
public record MariaDbDialect(ParameterStyle parameters) implements Dialect {

  public static final String NAME = "MariaDB";

  private static final Decorator DECORATOR = Decorator.of(Statement.class, new MariaDbLimits())
      .then(Decorator.of(Concat.class, concat -> (Expression) sql -> sql.append("CONCAT(")
          .render(concat.texts(), ", ").append(")")))
      .then(Decorator.of(Now.class, now -> (Expression) sql -> sql.append("UTC_TIMESTAMP(6)")))
      .then(Decorator.of(Shift.class, shift -> (Expression) sql -> sql.append("(").render(shift.timestamp())
          .append(" + INTERVAL " + shift.microseconds() + " MICROSECOND)")))
      .then(Decorator.of(OnConflict.class, MariaDbOnDuplicateKey::new))
      .then(Decorator.of(DefaultValues.class, values -> new MariaDbEmptyRow()))
      .then(Decorator.of(Include.One.class, MariaDbObject::new))
      .then(Decorator.of(Include.Many.class, MariaDbArray::new))
      .then(Decorator.of(Lock.class, MariaDbLock::new));

  public MariaDbDialect() {
    this(ParameterStyle.QUESTION_MARK);
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String name) {
    return "`" + name.replace("`", "``") + "`";
  }

  @Override
  public Decorator decorator() {
    return DECORATOR;
  }
}
