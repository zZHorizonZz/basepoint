package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.DeleteQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;

import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A delete of the rows of a table. Each step gives a new delete.
 */
public final class DeleteImpl implements DeleteQuery {

  private final Dialect dialect;
  private final String table;
  private final List<Filter> filters;
  private final boolean all;
  private final List<String> returning;

  private DeleteImpl(Dialect dialect, String table, List<Filter> filters, boolean all, List<String> returning) {
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
    return new DeleteImpl(dialect, table, filters, all, List.of(columns));
  }

  @Override
  public Statement build() {
    if (filters.isEmpty() && !all) {
      throw new IllegalStateException("a delete without a filter needs all(), so that it never empties the table by"
          + " mistake");
    }
    StatementWriter sql = new StatementWriter(dialect);
    sql.scoped(sql.quoteTable(table), () -> {
      sql.append("DELETE FROM ").table(table);
      if (returning != null) {
        sql.dialect().returning().head(sql, returning, true);
      }
      if (!filters.isEmpty()) {
        sql.append(" WHERE ").write(Filter.and(filters.toArray(Filter[]::new)));
      }
      if (returning != null) {
        sql.dialect().returning().tail(sql, returning, true);
      }
    });
    return sql.toStatement();
  }
}
