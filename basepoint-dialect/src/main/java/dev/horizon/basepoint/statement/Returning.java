package dev.horizon.basepoint.statement;

import java.util.List;

/**
 * {@code RETURNING}: the rows that an INSERT, UPDATE or DELETE wrote.
 *
 * @param columns the columns to return. An empty list returns all columns.
 */
public record Returning(List<String> columns) implements Clause {

  public Returning {
    columns = List.copyOf(columns);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("RETURNING ");
    if (columns.isEmpty()) {
      sql.append("*");
    }
    sql.names(columns);
  }
}
