package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.in;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.horizon.basepoint.InsertQuery;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class UpsertContractTest extends ContractTest {

  @Test
  void replacesExistingRowsAndInsertsNewRows() throws SQLException {
    execute(orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).replace("id"));

    assertThat(rows(orders.query().where(in("id", List.of(1, 6))).orderBy("id")))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(1L, "CLOSED", 999L), tuple(6L, "OPEN", 10L));
  }

  @Test
  void ignoresExistingRowsAndInsertsNewRows() throws SQLException {
    execute(orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).ignore("id"));

    assertThat(rows(orders.query().where(in("id", List.of(1, 6))).orderBy("id")))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(1L, "OPEN", 150L), tuple(6L, "OPEN", 10L));
  }

  @Test
  void returnsTheWrittenRows() throws SQLException {
    InsertQuery upsert = orders.insert(List.of(order(1, "CLOSED", 999), order(6, "OPEN", 10))).replace("id")
        .returning("id");

    assertThat(sortedIds(upsert)).containsExactly(1, 6);
  }
}
