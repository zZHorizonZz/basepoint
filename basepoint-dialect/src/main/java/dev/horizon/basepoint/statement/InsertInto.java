package dev.horizon.basepoint.statement;

import java.util.List;

/**
 * {@code INSERT INTO} with the statement's table and a column list. An empty list renders no column list, for a row
 * that uses the default value of every column.
 *
 * @param columns the columns that the inserted rows set
 */
public record InsertInto(List<String> columns) implements Clause {

  public InsertInto {
    columns = List.copyOf(columns);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("INSERT INTO " + sql.qualifier());
    if (!columns.isEmpty()) {
      sql.append(" (").names(columns).append(")");
    }
  }
}
