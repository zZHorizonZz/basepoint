package dev.horizon.basepoint;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.statement.Assignment;
import dev.horizon.basepoint.statement.Assignments;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Update;
import dev.horizon.basepoint.statement.Where;
import java.util.List;

/**
 * An UPDATE of rows. Create an update with {@link CollectionReference#update()}.
 *
 * <p>
 * An update needs at least one assignment, and either a filter or {@link #all()}. Otherwise {@link #build()} throws.
 * A later assignment to the same column replaces the earlier one. Updates are immutable. Each method returns a new
 * update.
 */
public final class UpdateQuery {

  private static final List<Class<? extends Clause>> ORDER = List.of(Update.class, Assignments.class, Where.class,
      Returning.class);

  private final Dialect dialect;
  private final Statement statement;
  private final boolean everyRow;

  private UpdateQuery(Dialect dialect, Statement statement, boolean everyRow) {
    this.dialect = dialect;
    this.statement = statement;
    this.everyRow = everyRow;
  }

  UpdateQuery(Dialect dialect, String table) {
    this(dialect, new Statement(new Source.Target(table), List.of(new Update(), new Assignments(List.of()))), false);
  }

  /**
   * Sets a column to a value.
   *
   * @param column the column name
   * @param value the value, an {@link Expression}, or {@code null}
   * @return a new update with the assignment
   */
  public UpdateQuery set(String column, Object value) {
    Assignments assignments = statement.find(Assignments.class).orElseThrow();
    return new UpdateQuery(dialect, statement.replace(assignments, assignments.put(new Assignment(column,
        Expression.value(value)))), everyRow);
  }

  /**
   * Adds a number to a column: {@code column = (column + amount)}.
   *
   * @param column the column name
   * @param amount the number to add, which can be negative
   * @return a new update with the assignment
   */
  public UpdateQuery increment(String column, Number amount) {
    return set(column, Expression.add(Expression.column(column), Expression.value(amount)));
  }

  /**
   * Sets a column to {@code NULL}.
   *
   * @param column the column name
   * @return a new update with the assignment
   */
  public UpdateQuery unset(String column) {
    return set(column, null);
  }

  /**
   * Adds a filter. Each call adds a filter, and a row must match all of them.
   *
   * @param filter the filter
   * @return a new update with the filter
   */
  public UpdateQuery where(Filter filter) {
    return new UpdateQuery(dialect, statement.where(filter), everyRow);
  }

  /**
   * Adds a filter written in the AIP-160 filter syntax. See {@link Filter#of(String)}.
   *
   * @param text the filter text, for example {@code state = "OPEN" AND total > 100}
   * @return a new update with the filter
   */
  public UpdateQuery where(String text) {
    return where(Filter.of(text));
  }

  /**
   * Updates every row of the table.
   *
   * @return a new update without a filter
   */
  public UpdateQuery all() {
    return new UpdateQuery(dialect, statement, true);
  }

  /**
   * Returns the updated rows, as they are after the update. PostgreSQL, MariaDB and SQLite render {@code RETURNING},
   * and SQL Server renders {@code OUTPUT}. MariaDB supports {@code UPDATE ... RETURNING} from version 13.0, and earlier
   * versions reject the statement.
   *
   * @param columns the columns to return. Pass none to return all columns.
   * @return a new update that returns the rows
   */
  public UpdateQuery returning(String... columns) {
    return new UpdateQuery(dialect, statement.put(new Returning(List.of(columns)), ORDER), everyRow);
  }

  /**
   * Returns the statement tree, before any decorator rewrites it.
   *
   * @return the statement
   * @throws IllegalStateException if the update sets no columns
   * @throws UnfilteredWriteException if the update has no filter and is not for {@link #all()} rows
   */
  public Statement statement() {
    if (statement.find(Assignments.class).orElseThrow().assignments().isEmpty()) {
      throw new IllegalStateException("The update sets no columns. Call set(), increment() or unset().");
    }
    if (!statement.has(Where.class) && !everyRow) {
      throw new UnfilteredWriteException("The update has no filter. Add a filter with where(), or call all() to"
          + " update every row.");
    }
    return statement;
  }

  /**
   * Renders the update in the dialect of the collection that created it.
   *
   * @return the SQL text and its placeholder values
   * @throws IllegalStateException if the update sets no columns
   * @throws UnfilteredWriteException if the update has no filter and is not for {@link #all()} rows
   */
  public RenderedStatement build() {
    return dialect.render(statement());
  }
}
