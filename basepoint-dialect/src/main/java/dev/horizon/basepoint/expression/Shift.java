package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * A timestamp moved by a duration, rendered as {@code (timestamp + INTERVAL 'n seconds')}, with the seconds to the
 * microsecond. Dialects whose database moves a timestamp otherwise decorate it.
 *
 * @param timestamp the timestamp
 * @param duration the duration, earlier when it is negative
 */
public record Shift(Expression timestamp, Duration duration) implements Expression {

  public Shift {
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(duration, "duration");
  }

  /**
   * Returns the duration in whole microseconds.
   *
   * @return the microseconds, negative for an earlier time
   */
  public long microseconds() {
    return Math.addExact(Math.multiplyExact(duration.getSeconds(), 1_000_000L), duration.getNano() / 1_000);
  }

  /**
   * Returns the duration in seconds, to the microsecond.
   *
   * @return the seconds, for example {@code -86400.000000}, negative for an earlier time
   */
  public BigDecimal seconds() {
    return BigDecimal.valueOf(microseconds(), 6);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(timestamp).append(" + INTERVAL '" + seconds().toPlainString() + " seconds')");
  }
}
