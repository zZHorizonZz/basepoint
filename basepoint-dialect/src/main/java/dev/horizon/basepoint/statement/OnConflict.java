package dev.horizon.basepoint.statement;

import java.util.List;

/**
 * {@code ON CONFLICT}. When an inserted row has the same key as an existing row, the existing row is updated or
 * kept unchanged.
 *
 * @param key the columns of the primary key or unique index
 * @param updates the columns to copy from the inserted row with {@code EXCLUDED}. An empty list renders
 * {@code DO NOTHING}.
 */
public record OnConflict(List<String> key, List<String> updates) implements Clause {

  public OnConflict {
    key = List.copyOf(key);
    updates = List.copyOf(updates);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("ON CONFLICT (").names(key).append(")");
    if (updates.isEmpty()) {
      sql.append(" DO NOTHING");
      return;
    }
    sql.append(" DO UPDATE SET ");
    for (int i = 0; i < updates.size(); i++) {
      sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = EXCLUDED.").name(updates.get(i));
    }
  }
}
