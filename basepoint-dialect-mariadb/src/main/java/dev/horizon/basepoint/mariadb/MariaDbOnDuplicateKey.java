package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.OnConflict;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

record MariaDbOnDuplicateKey(OnConflict conflict) implements Clause {

  @Override
  public void render(StatementWriter sql) {
    sql.append("ON DUPLICATE KEY UPDATE ");
    List<String> updates = conflict.updates();
    if (updates.isEmpty()) {
      String key = conflict.key().getFirst();
      sql.name(key).append(" = ").name(key);
      return;
    }
    for (int i = 0; i < updates.size(); i++) {
      sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = VALUES(").name(updates.get(i)).append(")");
    }
  }
}
