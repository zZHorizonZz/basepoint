package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpdateQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * The default {@link UpdateQuery}.
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
   * The state of an update.
   *
   * @param assignments the values of the columns, in order
   * @param filters the filters of WHERE
   * @param all true to update every row without a filter
   * @param returning the rows that it gives, or null for none
   */
  private record Steps(List<Clause.Assignment> assignments, List<Filter> filters, boolean all,
      Clause.Returning returning) {

    Steps withAssignment(Clause.Assignment assignment) {
      List<Clause.Assignment> more = new ArrayList<>(assignments);
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

    Steps withReturning(Clause.Returning rows) {
      return new Steps(assignments, filters, all, rows);
    }
  }

  private UpdateImpl with(Steps next) {
    return new UpdateImpl(dialect, table, next);
  }

  @Override
  public UpdateQuery set(String column, Object value) {
    return with(steps.withAssignment(new Clause.Assignment(column,
        value == null ? new Expression.Null() : Expression.value(value))));
  }

  @Override
  public UpdateQuery increment(String column, Number amount) {
    return with(steps.withAssignment(new Clause.Assignment(column,
        new Expression.Plus(Expression.column(column), Expression.value(amount)))));
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
    return with(steps.withReturning(new Clause.Returning(List.of(columns))));
  }

  @Override
  public Statement statement() {
    if (steps.assignments().isEmpty()) {
      throw new IllegalStateException("The update sets no columns. Call set(), increment() or unset().");
    }
    if (steps.filters().isEmpty() && !steps.all()) {
      throw new IllegalStateException("The update has no filter. Add a filter with where(), or call all() to"
          + " update every row.");
    }
    List<Clause> clauses = new ArrayList<>();
    clauses.add(new Clause.Update());
    clauses.add(new Clause.Set(steps.assignments()));
    if (!steps.filters().isEmpty()) {
      clauses.add(new Clause.Where(QueryImpl.and(steps.filters())));
    }
    if (steps.returning() != null) {
      clauses.add(steps.returning());
    }
    return new Statement(Source.named(table), clauses);
  }

  @Override
  public RenderedStatement build() {
    return new StatementWriter(dialect).render(statement()).rendered();
  }
}
