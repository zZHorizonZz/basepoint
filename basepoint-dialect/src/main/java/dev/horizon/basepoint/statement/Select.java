package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.expression.AllColumns;
import dev.horizon.basepoint.expression.Column;
import dev.horizon.basepoint.expression.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code SELECT} with its column list. An empty list renders all columns of the source, for example
 * {@code "t0".*}.
 *
 * @param distinct {@code true} to render {@code SELECT DISTINCT}
 * @param columns the columns
 */
public record Select(boolean distinct, List<Expression> columns) implements Clause {

  public Select {
    columns = List.copyOf(columns);
  }

  @Override
  public void declare(StatementWriter sql) {
    columns.forEach(column -> column.label().filter(label -> !column.equals(new Column(label))).ifPresent(sql::label));
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append(distinct ? "SELECT DISTINCT " : "SELECT ");
    renderColumns(sql);
  }

  /**
   * Renders the column list, or all columns of the source if the list is empty.
   *
   * @param sql the writer that receives the SQL
   */
  public void renderColumns(StatementWriter sql) {
    sql.render(columns.isEmpty() ? List.of(new AllColumns()) : columns, ", ");
  }

  /**
   * Returns whether this clause selects all columns of the source.
   *
   * @return {@code true} if the column list is empty or contains {@link AllColumns}
   */
  public boolean allColumns() {
    return columns.isEmpty() || columns.stream().anyMatch(AllColumns.class::isInstance);
  }

  /**
   * Returns a copy of this clause with a column added at the end. If this clause selects all columns through an
   * empty list, the copy keeps them with {@link AllColumns}.
   *
   * @param column the column to add
   * @return the new clause
   */
  /**
   * Returns a copy of this clause with more columns at the end of the list.
   *
   * @param more the columns
   * @return the new clause
   */
  public Select add(List<Expression> more) {
    List<Expression> all = new ArrayList<>(columns);
    all.addAll(more);
    return new Select(distinct, all);
  }

  /**
   * Returns a copy of this clause that gives each row once, or every row.
   *
   * @param distinct {@code true} to give each row once
   * @return the new clause
   */
  public Select distinct(boolean distinct) {
    return new Select(distinct, columns);
  }

  /**
   * Returns whether a column of the list has a name.
   *
   * @param name the name
   * @return {@code true} if one does
   */
  public boolean names(String name) {
    return columns.stream().anyMatch(column -> column.label().filter(name::equals).isPresent());
  }

  public Select with(Expression column) {
    List<Expression> more = new ArrayList<>(columns.isEmpty() ? List.of(new AllColumns()) : columns);
    more.add(column);
    return new Select(distinct, more);
  }
}
