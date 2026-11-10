package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.notExists;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class DeleteContractTest extends ContractTest {

  @Test
  @DisplayName("Deletes the rows that the filter keeps")
  void delete() throws SQLException {
    assertThat(execute(orders.delete().where(eq("state", "OPEN")))).isEqualTo(3);
    assertThat(ids(orders.query())).containsExactly(3, 4);
  }

  @Test
  @DisplayName("Deletes the rows that filter text keeps")
  void deleteByText() throws SQLException {
    assertThat(execute(orders.delete().where("state = OPEN AND total > 100"))).isEqualTo(2);
    assertThat(ids(orders.query())).containsExactly(2, 3, 4);
  }

  @Test
  @DisplayName("Deletes the rows without related rows")
  void deleteUnrelated() throws SQLException {
    assertThat(execute(orders.delete().where(notExists(items.query().on("order_id", "id"))))).isEqualTo(1);
    assertThat(ids(orders.query())).containsExactly(1, 2, 3, 5);
  }

  @Test
  @DisplayName("Deletes every row with all()")
  void deleteAll() throws SQLException {
    assertThat(execute(orders.delete().all())).isEqualTo(5);
    assertThat(ids(orders.query())).isEmpty();
  }

  @Test
  @DisplayName("Deletes no row when the filter keeps none")
  void deleteNothing() throws SQLException {
    assertThat(execute(orders.delete().where(eq("id", -1)))).isZero();
    assertThat(ids(orders.query())).hasSize(5);
  }

  @Test
  @DisplayName("Returns the deleted rows as they were")
  void returnDeleted() throws SQLException {
    List<Map<String, Object>> rows = rows(items.delete().where(eq("order_id", 1)).returning("id", "sku"));

    assertThat(rows).extracting(row -> row.get("sku")).containsExactlyInAnyOrder("MUG", "CUP", "a%b_c");
    assertThat(ids(items.query())).containsExactly(13, 14, 15);
  }

  @Test
  @DisplayName("Returns every column of the deleted rows without columns")
  void returnEverything() throws SQLException {
    List<Map<String, Object>> rows = rows(items.delete().where(eq("id", 13)).returning());

    assertThat(rows).singleElement().satisfies(row -> assertThat(row).containsKeys("id", "order_id", "sku", "qty")
        .containsEntry("sku", "MUG"));
  }
}
