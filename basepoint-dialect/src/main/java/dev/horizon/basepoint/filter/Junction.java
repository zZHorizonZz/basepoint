package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

record Junction(List<Filter> operands, String operator, String empty) {

  void render(StatementWriter sql) {
    if (operands.isEmpty()) {
      sql.append(empty);
      return;
    }
    sql.append("(");
    for (int i = 0; i < operands.size(); i++) {
      sql.append(i == 0 ? "" : operator).render(operands.get(i));
    }
    sql.append(")");
  }
}
