package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.InsertQuery;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpsertSyntax;

import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpsertSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An insert of rows, which every row sets the same columns of, in the order of the first row. Each step gives a new
 * insert.
 */
public final class InsertImpl implements InsertQuery, UpsertSyntax.Upsert {

  private final Dialect dialect;
  private final String table;
  private final List<Map<String, Expression>> rows;
  private final List<String> key;
  private final List<String> updates;
  private final List<String> returning;

  private InsertImpl(Dialect dialect, String table, List<Map<String, Expression>> rows, List<String> key,
      List<String> updates, List<String> returning) {
    this.dialect = dialect;
    this.table = table;
    this.rows = rows;
    this.key = key;
    this.updates = updates;
    this.returning = returning;
  }

  public InsertImpl(Dialect dialect, String table, List<? extends Map<String, ?>> rows) {
    this(dialect, table, values(rows), null, null, null);
  }

  private static List<Map<String, Expression>> values(List<? extends Map<String, ?>> rows) {
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("an insert has a row");
    }
    List<Map<String, Expression>> all = new ArrayList<>();
    for (Map<String, ?> row : rows) {
      if (!row.keySet().equals(rows.getFirst().keySet())) {
        throw new IllegalArgumentException("every row sets the columns " + rows.getFirst().keySet() + ", but row "
            + all.size() + " sets " + row.keySet());
      }
      Map<String, Expression> values = new LinkedHashMap<>();
      row.forEach((column, value) -> values.put(column, Expression.value(value)));
      all.add(values);
    }
    if (all.getFirst().isEmpty() && all.size() > 1) {
      throw new IllegalArgumentException("only one row takes the defaults of every column");
    }
    return List.copyOf(all);
  }

  @Override
  public InsertQuery ignore(String... key) {
    return new InsertImpl(dialect, table, rows, key(key), List.of(), returning);
  }

  @Override
  public InsertQuery replace(String... key) {
    List<String> columns = key(key);
    return new InsertImpl(dialect, table, rows, columns,
        columns().stream().filter(column -> !columns.contains(column)).toList(), returning);
  }

  @Override
  public InsertQuery returning(String... columns) {
    return new InsertImpl(dialect, table, rows, key, updates, List.of(columns));
  }

  @Override
  public Statement build() {
    StatementWriter sql = new StatementWriter(dialect);
    if (key == null) {
      writeInsert(sql);
      if (returning != null) {
        sql.dialect().returning().tail(sql, returning, false);
      }
    } else {
      sql.dialect().upserts().write(sql, this);
    }
    return sql.toStatement();
  }

  @Override
  public String table() {
    return table;
  }

  @Override
  public List<String> columns() {
    return List.copyOf(rows.getFirst().keySet());
  }

  @Override
  public List<String> key() {
    return key;
  }

  @Override
  public List<String> updates() {
    return updates;
  }

  @Override
  public List<String> returning() {
    return returning;
  }

  @Override
  public void writeInsert(StatementWriter sql) {
    List<String> columns = columns();
    sql.append("INSERT INTO ").table(table);
    if (!columns.isEmpty()) {
      sql.append(" (");
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? "" : ", ").name(columns.get(i));
      }
      sql.append(")");
    }
    if (returning != null) {
      sql.dialect().returning().head(sql, returning, false);
    }
    if (columns.isEmpty()) {
      sql.append(sql.dialect().defaultValues());
      return;
    }
    sql.append(" VALUES ");
    writeValues(sql);
  }

  @Override
  public void writeValues(StatementWriter sql) {
    List<String> columns = columns();
    for (int r = 0; r < rows.size(); r++) {
      sql.append(r == 0 ? "(" : ", (");
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? "" : ", ").write(rows.get(r).get(columns.get(i)));
      }
      sql.append(")");
    }
  }

  private List<String> key(String... columns) {
    if (columns.length == 0) {
      throw new IllegalArgumentException("a key has a column");
    }
    if (columns().isEmpty()) {
      throw new IllegalArgumentException("a row that takes the defaults of every column has no key to compare");
    }
    return List.of(columns);
  }
}
