package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.InsertInto;
import dev.horizon.basepoint.statement.OnConflict;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.StatementWriter;
import dev.horizon.basepoint.statement.Values;
import java.util.List;
import java.util.function.UnaryOperator;

final class SqlServerMerge implements UnaryOperator<Statement> {

  @Override
  public Statement apply(Statement statement) {
    return statement.find(OnConflict.class).map(conflict -> new Statement(statement.source(), List.of(
        new Merge(statement.find(InsertInto.class).orElseThrow(), statement.find(Values.class)
            .orElseThrow(), conflict,
            statement.find(Returning.class).stream().map(returning -> new SqlServerOutput.Output(
                "INSERTED", returning.columns())).toList()))))
        .orElse(statement);
  }

  record Merge(InsertInto insert, Values values, OnConflict conflict,
      List<SqlServerOutput.Output> output) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      String target = sql.alias();
      String source = sql.alias();
      List<String> columns = insert.columns();
      sql.append("MERGE INTO " + sql.qualifier() + " WITH (HOLDLOCK) AS " + target + " USING (").render(values)
          .append(") AS " + source + " (").names(columns).append(") ON ");
      List<String> key = conflict.key();
      for (int i = 0; i < key.size(); i++) {
        sql.append(i == 0 ? "" : " AND ").append(target + ".").name(key.get(i)).append(" = " + source + ".")
            .name(key.get(i));
      }
      List<String> updates = conflict.updates();
      if (!updates.isEmpty()) {
        sql.append(" WHEN MATCHED THEN UPDATE SET ");
        for (int i = 0; i < updates.size(); i++) {
          sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = " + source + ".").name(updates.get(i));
        }
      }
      sql.append(" WHEN NOT MATCHED THEN INSERT (").names(columns).append(") VALUES (");
      for (int i = 0; i < columns.size(); i++) {
        sql.append(i == 0 ? "" : ", ").append(source + ".").name(columns.get(i));
      }
      sql.append(")");
      for (SqlServerOutput.Output clause : output) {
        sql.append(" ").render(clause);
      }
      sql.append(";");
    }
  }
}
