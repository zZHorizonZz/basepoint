package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * Equality between a column of the current source and a column of the enclosing statement's source.
 *
 * @param column the column of the current source
 * @param outerColumn the column of the enclosing source
 */
public record Related(String column, String outerColumn) implements Filter {

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").column(column).append(" = ").outerColumn(outerColumn).append(")");
  }
}
