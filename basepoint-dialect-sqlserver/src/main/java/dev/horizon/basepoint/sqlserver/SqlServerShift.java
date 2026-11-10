package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.UnsupportedFeatureException;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Shift;
import dev.horizon.basepoint.statement.StatementWriter;

record SqlServerShift(Shift shift) implements Expression {

  SqlServerShift {
    long seconds = shift.duration().getSeconds();
    if (seconds < Integer.MIN_VALUE || seconds > Integer.MAX_VALUE) {
      throw new UnsupportedFeatureException("SQL Server moves a timestamp by at most 68 years, but the duration is "
          + shift.duration() + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("DATEADD(microsecond, " + shift.duration().getNano() / 1_000 + ", DATEADD(second, "
        + shift.duration().getSeconds() + ", ").render(shift.timestamp()).append("))");
  }
}
