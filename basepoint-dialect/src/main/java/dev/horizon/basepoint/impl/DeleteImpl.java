package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.DeleteQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The default {@link DeleteQuery}.
 */
public final class DeleteImpl implements DeleteQuery {

  private final Dialect dialect;
  private final String table;
  private final List<Filter> filters;
  private final boolean all;
  private final Clause.Returning returning;

  private DeleteImpl(Dialect dialect, String table, List<Filter> filters, boolean all, Clause.Returning returning) {
    this.dialect = dialect;
    this.table = table;
    this.filters = filters;
    this.all = all;
    this.returning = returning;
  }

  public DeleteImpl(Dialect dialect, String table) {
    this(dialect, table, List.of(), false, null);
  }

  @Override
  public DeleteQuery where(Filter filter) {
    List<Filter> more = new ArrayList<>(filters);
    more.add(filter);
    return new DeleteImpl(dialect, table, List.copyOf(more), all, returning);
  }

  @Override
  public DeleteQuery where(String filter) {
    return where(Filter.of(filter));
  }

  @Override
  public DeleteQuery all() {
    return new DeleteImpl(dialect, table, filters, true, returning);
  }

  @Override
  public DeleteQuery returning(String... columns) {
    return new DeleteImpl(dialect, table, filters, all, new Clause.Returning(List.of(columns)));
  }

  @Override
  public Statement statement() {
    if (filters.isEmpty() && !all) {
      throw new IllegalStateException("The delete has no filter. Add a filter with where(), or call all() to"
          + " delete every row.");
    }
    List<Clause> clauses = new ArrayList<>();
    clauses.add(new Clause.DeleteFrom());
    if (!filters.isEmpty()) {
      clauses.add(new Clause.Where(QueryImpl.and(filters)));
    }
    if (returning != null) {
      clauses.add(returning);
    }
    return new Statement(Source.named(table), clauses);
  }

  @Override
  public RenderedStatement build() {
    return new StatementWriter(dialect).render(statement()).rendered();
  }
}
