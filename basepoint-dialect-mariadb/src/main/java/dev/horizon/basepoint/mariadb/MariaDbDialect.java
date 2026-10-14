package dev.horizon.basepoint.mariadb;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Include;

/**
 * The dialect for MariaDB 11.4 and later.
 *
 * <p>
 * Identifiers use backticks. Placeholders default to {@code ?}. The dialect renders upserts as
 * {@code ON DUPLICATE KEY UPDATE} and includes as {@code JSON_ARRAYAGG}. It renders {@code RETURNING} for inserts,
 * updates and deletes. MariaDB supports {@code UPDATE ... RETURNING} from version 13.0.
 *
 * <p>
 * {@code JSON_ARRAYAGG} returns at most {@code group_concat_max_len} bytes, which is 1 MB by default. Raise that
 * setting if an include can return more.
 */
public final class MariaDbDialect implements Dialect {

  public static final String NAME = "MariaDB";

  private final ParameterStyle parameters;
  private final Decorator decorator = new MariaDbLimits()
      .then(Decorator.of(Clause.OnConflict.class, MariaDbOnDuplicateKey::new))
      .then(Decorator.of(Clause.DefaultValues.class, ignored -> new MariaDbEmptyRow()))
      .then(Decorator.of(Include.class, MariaDbJson::new));

  public MariaDbDialect() {
    this(ParameterStyle.QUESTION_MARK);
  }

  public MariaDbDialect(ParameterStyle parameters) {
    this.parameters = parameters;
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
  public ParameterStyle parameters() {
    return parameters;
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }
}
