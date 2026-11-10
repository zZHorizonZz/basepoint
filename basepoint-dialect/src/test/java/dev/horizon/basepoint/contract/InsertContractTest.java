package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class InsertContractTest extends ContractTest {

  @Test
  @DisplayName("Inserts one row")
  void insertOne() throws SQLException {
    assertThat(execute(orders.insert(order(6, "OPEN", 10)))).isEqualTo(1);
    assertThat(rows(orders.query().columns("id", "state", "total").where(gt("id", 5))))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(6L, "OPEN", 10L));
  }

  @Test
  @DisplayName("Inserts several rows in one statement")
  void insertMany() throws SQLException {
    assertThat(execute(orders.insert(List.of(order(6, "OPEN", 10), order(7, "CLOSED", 20))))).isEqualTo(2);
    assertThat(ids(orders.query().where(gt("id", 5)))).containsExactly(6, 7);
  }

  @Test
  @DisplayName("Inserts NULL for a null value")
  void insertNull() throws SQLException {
    Map<String, Object> row = order(6, "OPEN", 10);
    row.put("note", null);

    assertThat(execute(orders.insert(row))).isEqualTo(1);
    assertThat(ids(orders.query().where(eq("note", null)))).containsExactly(1, 4, 5, 6);
  }

  @Test
  @DisplayName("Inserts the value of an expression")
  void insertExpression() throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>(order(6, "OPEN", 0));
    row.put("total", add(value(40), value(2)));

    assertThat(execute(orders.insert(row))).isEqualTo(1);
    assertThat(number(rows(orders.query().columns("total").where(eq("id", 6))).getFirst().get("total")))
        .isEqualTo(42);
  }

  @Test
  @DisplayName("Inserts the default value of every column")
  void insertDefaults() throws SQLException {
    assertThat(execute(defaults.insert(Map.of()))).isEqualTo(1);
    assertThat(rows(defaults.query())).extracting(row -> number(row.get("id")), row -> row.get("label"))
        .containsExactly(tuple(7L, "x"));
  }

  @Test
  @DisplayName("Returns the inserted rows")
  void returnInserted() throws SQLException {
    assertThat(rows(orders.insert(List.of(order(6, "OPEN", 10), order(7, "OPEN", 20))).returning("id", "state")))
        .extracting(row -> number(row.get("id")), row -> row.get("state"))
        .containsExactlyInAnyOrder(tuple(6L, "OPEN"), tuple(7L, "OPEN"));
  }
}
