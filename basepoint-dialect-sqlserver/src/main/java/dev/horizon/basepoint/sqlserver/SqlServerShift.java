package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.StatementWriter;

/**
 * A timestamp moved by a duration, rendered with {@code DATEADD}. {@code DATEADD} takes an {@code int}, so a duration in
 * microseconds overflows after 35 minutes: the timestamp moves by the whole seconds first, and then by the
 * microseconds that are left.
 *
 * @param shift the timestamp and the duration
 */
record SqlServerShift(Expression.Shift shift) implements Expression {

  SqlServerShift {
    long seconds = shift.duration().getSeconds();
    if (seconds < Integer.MIN_VALUE || seconds > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("SQL Server moves a timestamp by at most 68 years, but the duration is "
          + shift.duration() + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("DATEADD(microsecond, " + shift.duration().getNano() / 1_000 + ", DATEADD(second, "
        + shift.duration().getSeconds() + ", ").render(shift.timestamp()).append("))");
  }
}
