package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.InsertQuery;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The default {@link InsertQuery}.
 */
public final class InsertImpl implements InsertQuery {

  private final Dialect dialect;
  private final String table;
  private final List<String> columns;
  private final List<List<Expression>> rows;
  private final Clause.OnConflict conflict;
  private final Clause.Returning returning;

  private InsertImpl(Dialect dialect, String table, List<String> columns, List<List<Expression>> rows,
      Clause.OnConflict conflict, Clause.Returning returning) {
    this.dialect = dialect;
    this.table = table;
    this.columns = columns;
    this.rows = rows;
    this.conflict = conflict;
    this.returning = returning;
  }

  public InsertImpl(Dialect dialect, String table, List<? extends Map<String, ?>> rows) {
    this(dialect, table, columns(rows), values(rows), null, null);
  }

  private static List<String> columns(List<? extends Map<String, ?>> rows) {
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("The insert has no rows.");
    }
    return List.copyOf(rows.getFirst().keySet());
  }

  private static List<List<Expression>> values(List<? extends Map<String, ?>> rows) {
    List<String> columns = columns(rows);
    if (columns.isEmpty() && rows.size() > 1) {
      throw new IllegalArgumentException("Only one row can use the default value of every column.");
    }
    List<List<Expression>> all = new ArrayList<>();
    for (Map<String, ?> row : rows) {
      if (!row.keySet().equals(rows.getFirst().keySet())) {
        throw new IllegalArgumentException("Row " + all.size() + " sets the columns " + row.keySet()
            + ", but the first row sets " + columns + ". Every row must set the same columns.");
      }
      all.add(columns.stream().map(column -> Expression.value(row.get(column))).toList());
    }
    return List.copyOf(all);
  }

  @Override
  public InsertQuery ignore(String... key) {
    return new InsertImpl(dialect, table, columns, rows, new Clause.OnConflict(key(key), List.of()), returning);
  }

  @Override
  public InsertQuery replace(String... key) {
    List<String> columnsOfKey = key(key);
    return new InsertImpl(dialect, table, columns, rows, new Clause.OnConflict(columnsOfKey,
        columns.stream().filter(column -> !columnsOfKey.contains(column)).toList()), returning);
  }

  @Override
  public InsertQuery returning(String... columns) {
    return new InsertImpl(dialect, table, this.columns, rows, conflict, new Clause.Returning(List.of(columns)));
  }

  @Override
  public Statement statement() {
    List<Clause> clauses = new ArrayList<>();
    clauses.add(new Clause.InsertInto(columns));
    clauses.add(columns.isEmpty() ? new Clause.DefaultValues() : new Clause.Values(rows));
    if (conflict != null) {
      clauses.add(conflict);
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

  private List<String> key(String... key) {
    if (key.length == 0) {
      throw new IllegalArgumentException("The key has no columns.");
    }
    if (columns.isEmpty()) {
      throw new IllegalArgumentException("A row that uses the default value of every column cannot use a key.");
    }
    return List.of(key);
  }
}
