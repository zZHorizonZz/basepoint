package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/**
 * {@code ON DUPLICATE KEY UPDATE}, the MariaDB form of {@link Clause.OnConflict}.
 *
 * <p>
 * {@code VALUES(column)} refers to the value of the inserted row. MariaDB matches any unique key of the table, not
 * only the key of the upsert. To keep an existing row unchanged, the clause sets the first key column to its own
 * value.
 *
 * @param conflict the upsert
 */
record MariaDbOnDuplicateKey(Clause.OnConflict conflict) implements Clause {

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
