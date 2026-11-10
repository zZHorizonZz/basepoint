package dev.horizon.basepoint;

import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.DeleteFrom;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Where;
import java.util.List;

/**
 * A DELETE of rows. Create a delete with {@link CollectionReference#delete()}.
 *
 * <p>
 * A delete needs a filter or {@link #all()}. Otherwise {@link #build()} throws {@link UnfilteredWriteException}.
 * Deletes are immutable. Each method returns a new delete.
 */
public final class DeleteQuery {

  private static final List<Class<? extends Clause>> ORDER = List.of(DeleteFrom.class, Where.class, Returning.class);

  private final Dialect dialect;
  private final Statement statement;
  private final boolean everyRow;

  private DeleteQuery(Dialect dialect, Statement statement, boolean everyRow) {
    this.dialect = dialect;
    this.statement = statement;
    this.everyRow = everyRow;
  }

  DeleteQuery(Dialect dialect, String table) {
    this(dialect, new Statement(new Source.Target(table), List.of(new DeleteFrom())), false);
  }

  /**
   * Adds a filter. Each call adds a filter, and a row must match all of them.
   *
   * @param filter the filter
   * @return a new delete with the filter
   */
  public DeleteQuery where(Filter filter) {
    return new DeleteQuery(dialect, statement.where(filter), everyRow);
  }

  /**
   * Adds a filter written in the AIP-160 filter syntax. See {@link Filter#of(String)}.
   *
   * @param text the filter text, for example {@code state = "OPEN" AND total > 100}
   * @return a new delete with the filter
   */
  public DeleteQuery where(String text) {
    return where(Filter.of(text));
  }

  /**
   * Deletes every row of the table.
   *
   * @return a new delete without a filter
   */
  public DeleteQuery all() {
    return new DeleteQuery(dialect, statement, true);
  }

  /**
   * Returns the deleted rows, as they were before the delete. PostgreSQL, MariaDB and SQLite render
   * {@code RETURNING}, and SQL Server renders {@code OUTPUT}.
   *
   * @param columns the columns to return. Pass none to return all columns.
   * @return a new delete that returns the rows
   */
  public DeleteQuery returning(String... columns) {
    return new DeleteQuery(dialect, statement.put(new Returning(List.of(columns)), ORDER), everyRow);
  }

  /**
   * Returns the statement tree, before any decorator rewrites it.
   *
   * @return the statement
   * @throws UnfilteredWriteException if the delete has no filter and is not for {@link #all()} rows
   */
  public Statement statement() {
    if (!statement.has(Where.class) && !everyRow) {
      throw new UnfilteredWriteException("The delete has no filter. Add a filter with where(), or call all() to"
          + " delete every row.");
    }
    return statement;
  }

  /**
   * Renders the delete in the dialect of the collection that created it.
   *
   * @return the SQL text and its placeholder values
   * @throws UnfilteredWriteException if the delete has no filter and is not for {@link #all()} rows
   */
  public RenderedStatement build() {
    return dialect.render(statement());
  }
}
