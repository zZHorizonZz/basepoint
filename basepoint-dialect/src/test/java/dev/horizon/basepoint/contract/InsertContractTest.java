package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.horizon.basepoint.InsertQuery;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class InsertContractTest extends ContractTest {

  @Test
  void insertsOneRow() throws SQLException {
    assertThat(execute(orders.insert(order(6, "OPEN", 10)))).isEqualTo(1);

    assertThat(ids(orders.query().where(gt("id", 5)))).containsExactly(6);
  }

  @Test
  void insertsSeveralRows() throws SQLException {
    assertThat(execute(orders.insert(List.of(order(6, "OPEN", 10), order(7, "CLOSED", 20))))).isEqualTo(2);

    assertThat(ids(orders.query().where(gt("id", 5)))).containsExactly(6, 7);
  }

  @Test
  void insertsTheDefaultsOfEveryColumn() throws SQLException {
    execute(defaults.insert(Map.of()));

    assertThat(rows(defaults.query())).extracting(row -> number(row.get("id")), row -> row.get("label"))
        .containsExactly(tuple(7L, "x"));
  }

  @Test
  void returnsTheInsertedRows() throws SQLException {
    InsertQuery insert = orders.insert(List.of(order(6, "OPEN", 10), order(7, "OPEN", 20))).returning("id", "state");

    assertThat(rows(insert)).extracting(row -> number(row.get("id")), row -> row.get("state"))
        .containsExactlyInAnyOrder(tuple(6L, "OPEN"), tuple(7L, "OPEN"));
  }
}
