package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.statement.Assignments;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.DeleteFrom;
import dev.horizon.basepoint.statement.InsertInto;
import dev.horizon.basepoint.statement.OnConflict;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;
import java.util.function.UnaryOperator;

final class SqlServerOutput implements UnaryOperator<Statement> {

  @Override
  public Statement apply(Statement statement) {
    if (statement.has(OnConflict.class)) {
      return statement;
    }
    return statement.find(Returning.class).map(returning -> {
      Statement rest = statement.remove(returning);
      if (rest.has(DeleteFrom.class)) {
        return rest.after(DeleteFrom.class, new Output("DELETED", returning.columns()));
      }
      Output output = new Output("INSERTED", returning.columns());
      return rest.has(Assignments.class) ? rest.after(Assignments.class, output)
          : rest.after(InsertInto.class,
              output);
    }).orElse(statement);
  }

  record Output(String rows, List<String> columns) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("OUTPUT ");
      if (columns.isEmpty()) {
        sql.append(rows + ".*");
      }
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? rows + "." : ", " + rows + ".").name(columns.get(i));
      }
    }
  }
}
