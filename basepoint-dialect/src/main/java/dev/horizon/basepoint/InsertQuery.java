package dev.horizon.basepoint;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.DefaultValues;
import dev.horizon.basepoint.statement.InsertInto;
import dev.horizon.basepoint.statement.OnConflict;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Values;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An INSERT of one or more rows. Create an insert with {@link CollectionReference#insert}.
 *
 * <p>
 * By default, a row with the same key as an existing row makes the database reject the insert. Use {@link #ignore}
 * or {@link #replace} to handle such rows. Inserts are immutable. Each method returns a new insert.
 */
public final class InsertQuery {

  private static final List<Class<? extends Clause>> ORDER = List.of(InsertInto.class, Values.class,
      DefaultValues.class, OnConflict.class, Returning.class);

  private final Dialect dialect;
  private final Statement statement;

  private InsertQuery(Dialect dialect, Statement statement) {
    this.dialect = dialect;
    this.statement = statement;
  }

  InsertQuery(Dialect dialect, String table, List<? extends Map<String, ?>> rows) {
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("The insert has no rows.");
    }
    List<String> columns = List.copyOf(rows.getFirst().keySet());
    if (columns.isEmpty() && rows.size() > 1) {
      throw new IllegalArgumentException("Only one row can use the default value of every column.");
    }
    List<List<Expression>> values = new ArrayList<>();
    for (Map<String, ?> row : rows) {
      if (!row.keySet().equals(rows.getFirst().keySet())) {
        throw new IllegalArgumentException("Row " + values.size() + " sets the columns " + row.keySet()
            + ", but the first row sets " + columns + ". Every row must set the same columns.");
      }
      values.add(columns.stream().map(column -> Expression.value(row.get(column))).toList());
    }
    this.dialect = dialect;
    this.statement = new Statement(new Source.Target(table), List.of(new InsertInto(columns), columns.isEmpty()
        ? new DefaultValues()
        : new Values(values)));
  }

  /**
   * Skips each row whose key matches an existing row.
   *
   * @param key the columns of the primary key or unique index. MariaDB uses any unique key of the table.
   * @return a new insert that skips such rows
   * @throws IllegalArgumentException if {@code key} is empty or the insert sets no columns
   */
  public InsertQuery ignore(String... key) {
    return put(new OnConflict(requireKey(key), List.of()));
  }

  /**
   * Updates the existing row when a row's key matches. The existing row takes all inserted columns except the key.
   *
   * @param key the columns of the primary key or unique index. MariaDB uses any unique key of the table.
   * @return a new insert that updates such rows
   * @throws IllegalArgumentException if {@code key} is empty or the insert sets no columns
   */
  public InsertQuery replace(String... key) {
    List<String> keyColumns = requireKey(key);
    return put(new OnConflict(keyColumns, columns().stream().filter(column -> !keyColumns.contains(column))
        .toList()));
  }

  /**
   * Returns the inserted rows, as they are after the insert. PostgreSQL, MariaDB and SQLite render
   * {@code RETURNING}, and SQL Server renders {@code OUTPUT}.
   *
   * @param columns the columns to return. Pass none to return all columns.
   * @return a new insert that returns the rows
   */
  public InsertQuery returning(String... columns) {
    return put(new Returning(List.of(columns)));
  }

  /**
   * Returns the statement tree, before any decorator rewrites it.
   *
   * @return the statement
   */
  public Statement statement() {
    return statement;
  }

  /**
   * Renders the insert in the dialect of the collection that created it.
   *
   * @return the SQL text and its placeholder values
   */
  public RenderedStatement build() {
    return dialect.render(statement);
  }

  private InsertQuery put(Clause clause) {
    return new InsertQuery(dialect, statement.put(clause, ORDER));
  }

  private List<String> columns() {
    return statement.find(InsertInto.class).orElseThrow().columns();
  }

  private List<String> requireKey(String... key) {
    if (key.length == 0) {
      throw new IllegalArgumentException("The key has no columns.");
    }
    if (columns().isEmpty()) {
      throw new IllegalArgumentException("A row that uses the default value of every column cannot use a key.");
    }
    return List.of(key);
  }
}
