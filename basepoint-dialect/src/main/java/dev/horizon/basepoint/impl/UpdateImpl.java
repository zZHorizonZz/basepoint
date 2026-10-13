package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpdateQuery;

import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An update of the rows of a table. Each step gives a new update.
 */
public final class UpdateImpl implements UpdateQuery {

  private final Dialect dialect;
  private final String table;
  private final Steps steps;

  private UpdateImpl(Dialect dialect, String table, Steps steps) {
    this.dialect = dialect;
    this.table = table;
    this.steps = steps;
  }

  public UpdateImpl(Dialect dialect, String table) {
    this(dialect, table, new Steps(List.of(), List.of(), false, null));
  }

  /**
   * What the steps of an update gave.
   *
   * @param assignments the values of the columns, in order
   * @param filters the filters of WHERE
   * @param all true to update every row without a filter
   * @param returning the columns of the rows that it gives, empty for all, or null for none
   */
  private record Steps(List<Assignment> assignments, List<Filter> filters, boolean all, List<String> returning) {

    Steps withAssignment(Assignment assignment) {
      List<Assignment> more = new ArrayList<>(assignments);
      more.removeIf(other -> other.column().equals(assignment.column()));
      more.add(assignment);
      return new Steps(List.copyOf(more), filters, all, returning);
    }

    Steps withFilter(Filter filter) {
      List<Filter> more = new ArrayList<>(filters);
      more.add(filter);
      return new Steps(assignments, List.copyOf(more), all, returning);
    }

    Steps withAll() {
      return new Steps(assignments, filters, true, returning);
    }

    Steps withReturning(List<String> columns) {
      return new Steps(assignments, filters, all, columns);
    }
  }

  /**
   * The value of a column.
   *
   * @param column the name of the column
   * @param value the value
   */
  private record Assignment(String column, Expression value) {
  }

  private UpdateImpl with(Steps next) {
    return new UpdateImpl(dialect, table, next);
  }

  @Override
  public UpdateQuery set(String column, Object value) {
    return with(steps.withAssignment(new Assignment(column,
        value == null ? sql -> sql.append("NULL") : Expression.value(value))));
  }

  @Override
  public UpdateQuery increment(String column, Number amount) {
    return with(steps.withAssignment(new Assignment(column,
        sql -> sql.column(column).append(" + ").param(amount))));
  }

  @Override
  public UpdateQuery unset(String column) {
    return set(column, null);
  }

  @Override
  public UpdateQuery where(Filter filter) {
    return with(steps.withFilter(filter));
  }

  @Override
  public UpdateQuery where(String filter) {
    return where(Filter.of(filter));
  }

  @Override
  public UpdateQuery all() {
    return with(steps.withAll());
  }

  @Override
  public UpdateQuery returning(String... columns) {
    return with(steps.withReturning(List.of(columns)));
  }

  @Override
  public Statement build() {
    if (steps.assignments().isEmpty()) {
      throw new IllegalStateException("an update sets a column");
    }
    if (steps.filters().isEmpty() && !steps.all()) {
      throw new IllegalStateException("an update without a filter needs all(), so that it never updates every row"
          + " by mistake");
    }
    StatementWriter sql = new StatementWriter(dialect);
    sql.scoped(sql.quoteTable(table), () -> {
      sql.append("UPDATE ").table(table).append(" SET ");
      List<Assignment> assignments = steps.assignments();
      for (int i = 0; i < assignments.size(); i++) {
        sql.append(i == 0 ? "" : ", ").name(assignments.get(i).column()).append(" = ")
            .write(assignments.get(i).value());
      }
      if (steps.returning() != null) {
        sql.dialect().returning().head(sql, steps.returning(), false);
      }
      if (!steps.filters().isEmpty()) {
        sql.append(" WHERE ").write(Filter.and(steps.filters().toArray(Filter[]::new)));
      }
      if (steps.returning() != null) {
        sql.dialect().returning().tail(sql, steps.returning(), false);
      }
    });
    return sql.toStatement();
  }
}
