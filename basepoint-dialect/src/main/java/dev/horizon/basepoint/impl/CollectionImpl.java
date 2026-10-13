package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.DeleteQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.InsertQuery;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.UpdateQuery;

import java.util.List;
import java.util.Map;

/**
 * The rows of a table, which start the builders of its statements.
 */
public final class CollectionImpl implements CollectionReference {

  private final Dialect dialect;
  private final String table;

  public CollectionImpl(Dialect dialect, String table) {
    this.dialect = dialect;
    this.table = table;
  }

  @Override
  public String name() {
    return table;
  }

  @Override
  public Query query() {
    return new QueryImpl(dialect, table);
  }

  @Override
  public InsertQuery insert(Map<String, ?> row) {
    return insert(List.of(row));
  }

  @Override
  public InsertQuery insert(List<? extends Map<String, ?>> rows) {
    return new InsertImpl(dialect, table, rows);
  }

  @Override
  public UpdateQuery update() {
    return new UpdateImpl(dialect, table);
  }

  @Override
  public DeleteQuery delete() {
    return new DeleteImpl(dialect, table);
  }
}
