package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.horizon.basepoint.UpdateQuery;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class UpdateContractTest extends ContractTest {

  @Test
  void setsIncrementsAndUnsetsColumns() throws SQLException {
    int updated = execute(orders.update().set("state", "CLOSED").increment("total", 5).unset("note")
        .where(eq("id", 2)));

    Map<String, Object> row = rows(orders.query().where(eq("id", 2))).getFirst();
    assertThat(updated).isEqualTo(1);
    assertThat(row.get("state")).isEqualTo("CLOSED");
    assertThat(number(row.get("total"))).isEqualTo(85L);
    assertThat(row.get("note")).isNull();
  }

  @Test
  void updatesRowsWithRelatedRows() throws SQLException {
    int updated = execute(orders.update().set("note", "big").where(exists(items.query().on("order_id", "id")
        .where(eq("sku", "MUG")))));

    assertThat(updated).isEqualTo(2);
    assertThat(ids(orders.query().where(eq("note", "big")))).containsExactly(1, 2);
  }

  @Test
  void updatesEveryRowOnlyWithAll() throws SQLException {
    assertThatThrownBy(() -> orders.update().set("note", "x").build()).isInstanceOf(IllegalStateException.class);

    assertThat(execute(orders.update().set("note", "x").all())).isEqualTo(5);
  }

  @Test
  void returnsTheUpdatedRows() throws SQLException {
    UpdateQuery update = orders.update().increment("total", 1).where("state = OPEN").returning("id", "total");

    if (!database.returnsUpdatedRows()) {
      assertThatThrownBy(() -> rows(update)).isInstanceOf(SQLException.class);
      return;
    }
    assertThat(rows(update)).extracting(row -> number(row.get("id")), row -> number(row.get("total")))
        .containsExactlyInAnyOrder(tuple(1L, 151L), tuple(2L, 81L), tuple(5L, 221L));
  }
}
