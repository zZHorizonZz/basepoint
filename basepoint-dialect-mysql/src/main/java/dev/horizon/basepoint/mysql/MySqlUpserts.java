package dev.horizon.basepoint.mysql;

import dev.horizon.basepoint.StatementWriter;
import dev.horizon.basepoint.UpsertSyntax;
import dev.horizon.basepoint.UpsertSyntax.Upsert;
import java.util.List;

/**
 * The upsert of MySQL: {@code ON DUPLICATE KEY UPDATE}, with the alias of the row that is not inserted. MySQL takes
 * any unique key of the table; to do nothing, it sets a column of the key to itself.
 */
final class MySqlUpserts extends UpsertSyntax {

  @Override
  public void write(StatementWriter sql, Upsert insert) {
    insert.writeInsert(sql);
    String excluded = sql.alias();
    sql.append(" AS " + excluded + " ON DUPLICATE KEY UPDATE ");
    List<String> updates = insert.updates();
    if (updates.isEmpty()) {
      String key = insert.key().getFirst();
      sql.name(key).append(" = ").table(insert.table()).append(".").name(key);
      return;
    }
    for (int i = 0; i < updates.size(); i++) {
      sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = " + excluded + ".").name(updates.get(i));
    }
  }
}
