package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.in;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class UpsertContractTest extends ContractTest {

  @Test
  @DisplayName("Updates the rows whose key exists and inserts the others")
  void replace() throws SQLException {
    execute(orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).replace("id"));

    assertThat(rows(orders.query().where(in("id", List.of(1, 6))).orderBy("id")))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(1L, "CLOSED", 999L), tuple(6L, "OPEN", 10L));
    assertThat(ids(orders.query())).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  @DisplayName("Keeps the rows whose key exists and inserts the others")
  void ignore() throws SQLException {
    execute(orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).ignore("id"));

    assertThat(rows(orders.query().where(in("id", List.of(1, 6))).orderBy("id")))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(1L, "OPEN", 150L), tuple(6L, "OPEN", 10L));
  }

  @Test
  @DisplayName("Keeps every row when every key exists")
  void ignoreAll() throws SQLException {
    execute(orders.insert(List.of(order(1, "X", 1), order(2, "X", 2))).ignore("id"));

    assertThat(valuesOf("state", rows(orders.query().where(in("id", List.of(1, 2))).orderBy("id"))))
        .containsExactly("OPEN", "OPEN");
  }

  @Test
  @DisplayName("Returns the written rows")
  void returnWritten() throws SQLException {
    assertThat(sortedIds(orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).replace("id")
        .returning("id").build())).containsExactly(1, 6);
  }
}
