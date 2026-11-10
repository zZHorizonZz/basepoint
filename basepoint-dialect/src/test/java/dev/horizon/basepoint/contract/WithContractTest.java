package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static dev.horizon.basepoint.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class WithContractTest extends ContractTest {

  private CollectionReference big;

  @BeforeEach
  void createCollections() {
    big = dialect.collection("big");
  }

  @Test
  @DisplayName("Reads a WITH query as a table")
  void read() throws SQLException {
    Query query = big.query().with("big", orders.query().columns("id", "total").where(gt("total", 100))).columns("id");

    assertThat(ids(query)).containsExactly(1, 3, 5);
  }

  @Test
  @DisplayName("Reads a WITH query that reads an earlier one")
  void readEarlier() throws SQLException {
    Query query = dialect.collection("big_open").query()
        .with("open", orders.query().columns("id", "total").where(eq("state", "OPEN")))
        .with("big_open", dialect.collection("open").query().where(gt("total", 100)))
        .columns("id");

    assertThat(ids(query)).containsExactly(1, 5);
  }

  @Test
  @DisplayName("Reads a WITH query in a filter and in an include")
  void readNested() throws SQLException {
    CollectionReference large = dialect.collection("large_items");
    Query query = orders.query().with("large_items", items.query().columns("id", "order_id", "sku")
        .where(gt("qty", 2))).columns("id").where(exists(large.query().on("order_id", "id")))
        .include("large", large.query().on("order_id", "id").columns("sku").orderBy("sku"));

    List<Map<String, Object>> rows = rows(query.orderBy("id"));

    assertThat(rows).extracting(row -> (int) number(row.get("id"))).containsExactly(1, 2, 5);
    assertThat(json(rows.getFirst().get("large"))).isEqualTo(json("[{\"sku\": \"a%b_c\"}]"));
  }

  @Test
  @DisplayName("Reads a WITH query of a union")
  void readUnion() throws SQLException {
    Query query = dialect.collection("all_ids").query()
        .with("all_ids", orders.query().columns("id").union(archived.query().columns("id"))).count();

    assertThat(number(rows(query).getFirst().get("count"))).isEqualTo(8);
  }

  @Test
  @DisplayName("Keeps the sort order and the limit of a WITH query")
  void keepLimit() throws SQLException {
    Query query = dialect.collection("top").query()
        .with("top", orders.query().columns("id", "total").orderBy("total", Direction.DESC).limit(2)).columns("id");

    assertThat(ids(query)).containsExactly(3, 5);
  }

  @Test
  @DisplayName("Pages a query that has a WITH query")
  void page() throws SQLException {
    Query query = big.query().with("big", orders.query().columns("id", "total").where(gt("total", 100))).columns("id")
        .orderBy("id").limit(2).offset(1);

    assertThat(idsInOrder(query)).containsExactly(3, 5);
  }

  @Test
  @DisplayName("Refuses a WITH query in a query inside another statement")
  void refuseNested() {
    Query nested = items.query().with("big", orders.query()).on("order_id", "id").columns("sku");

    assertThatThrownBy(() -> orders.query().include("items", nested)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("outermost query");
  }

  @Test
  @DisplayName("Refuses a WITH query that relates its rows with on()")
  void refuseOn() {
    assertThatThrownBy(() -> big.query().with("big", orders.query().on("id", "order_id")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Leave out on()");
  }

  @Test
  @DisplayName("Refuses two WITH queries with one name")
  void refuseDuplicate() {
    assertThatThrownBy(() -> big.query().with("big", orders.query()).with("big", items.query()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("two WITH queries named big");
  }
}
