package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Node;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/**
 * Rewrites {@link Clause.Limit} and {@link Clause.Offset} into SQL Server syntax.
 *
 * <p>
 * A limit without an offset becomes {@code SELECT TOP (n)}. An offset becomes {@code OFFSET ... ROWS FETCH NEXT ...
 * ROWS ONLY}. SQL Server requires {@code ORDER BY} with {@code OFFSET}, so a statement without a sort order gets
 * {@code ORDER BY (SELECT NULL)}.
 */
final class SqlServerPaging implements Decorator {

  @Override
  public Node decorate(Node node) {
    if (!(node instanceof Statement statement)) {
      return node;
    }
    Clause.Limit limit = statement.find(Clause.Limit.class).orElse(null);
    Clause.Offset offset = statement.find(Clause.Offset.class).orElse(null);
    if (offset != null) {
      Statement paged = limit == null ? statement : statement.remove(limit);
      paged = paged.replace(offset, new OffsetFetch(offset.rows(), limit == null ? null : limit.rows()));
      if (!paged.has(Clause.OrderBy.class)) {
        paged = paged.before(OffsetFetch.class,
            new Clause.OrderBy(List.of(new Clause.Sort(new NoOrder(), Direction.ASC))));
      }
      return paged;
    }
    if (limit != null) {
      Statement top = statement.remove(limit);
      Clause.Select select = top.find(Clause.Select.class).orElseThrow();
      return top.replace(select, new Top(select, limit.rows()));
    }
    return statement;
  }

  /**
   * {@code SELECT TOP (n)}.
   *
   * @param select the original SELECT clause
   * @param rows the maximum number of rows
   */
  record Top(Clause.Select select, long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append(select.distinct() ? "SELECT DISTINCT " : "SELECT ").append("TOP (" + rows + ") ");
      select.renderColumns(sql);
    }
  }

  /**
   * {@code OFFSET ... ROWS FETCH NEXT ... ROWS ONLY}.
   *
   * @param offset the number of rows to skip
   * @param limit the maximum number of rows, or {@code null} for no limit
   */
  record OffsetFetch(long offset, Long limit) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("OFFSET " + offset + " ROWS");
      if (limit != null) {
        sql.append(" FETCH NEXT " + limit + " ROWS ONLY");
      }
    }
  }

  /** {@code (SELECT NULL)}, a sort key that keeps the order of the database. */
  record NoOrder() implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(SELECT NULL)");
    }
  }
}
