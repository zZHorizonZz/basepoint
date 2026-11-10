package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.OrderBy;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Sort;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;
import java.util.function.UnaryOperator;

final class SqlServerPaging implements UnaryOperator<Statement> {

  @Override
  public Statement apply(Statement statement) {
    return statement.find(Offset.class).map(offset -> fetch(statement, offset))
        .or(() -> statement.find(Limit.class).map(limit -> top(statement, limit))).orElse(statement);
  }

  private Statement fetch(Statement statement, Offset offset) {
    Statement skip = statement.replace(offset, new OffsetRows(offset.rows()));
    Statement fetch = skip.find(Limit.class).map(limit -> skip.remove(limit).after(OffsetRows.class,
        new FetchNext(limit.rows()))).orElse(skip);
    return fetch.has(OrderBy.class) ? fetch
        : fetch.before(OffsetRows.class, new OrderBy(List.of(
            new Sort(new NoOrder(), Direction.ASC))));
  }

  private Statement top(Statement statement, Limit limit) {
    Statement rows = statement.remove(limit);
    Select select = rows.find(Select.class).orElseThrow(() -> new IllegalStateException("A limit of"
        + " SQL Server renders as TOP, which only a SELECT has."));
    return rows.replace(select, new Top(select, limit.rows()));
  }

  record Top(Select select, long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append(select.distinct() ? "SELECT DISTINCT " : "SELECT ").append("TOP (" + rows + ") ");
      select.renderColumns(sql);
    }
  }

  record OffsetRows(long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("OFFSET " + rows + " ROWS");
    }
  }

  record FetchNext(long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("FETCH NEXT " + rows + " ROWS ONLY");
    }
  }

  record NoOrder() implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(SELECT NULL)");
    }
  }
}
