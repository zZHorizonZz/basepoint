package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static dev.horizon.basepoint.Filter.gte;
import static dev.horizon.basepoint.Filter.in;
import static dev.horizon.basepoint.Filter.notExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class IncludeContractTest extends ContractTest {

  @Test
  void includesSortedAndLimitedRows() throws SQLException {
    Query lines = items.query().on("order_id", "id").columns("id", "sku", "qty").orderBy("qty", Direction.DESC)
        .limit(2);

    List<Map<String, Object>> rows = rows(orders.query().where(in("id", List.of(1, 4))).columns("id")
        .include("items", lines).orderBy("id"));

    assertThat(json(rows.get(0).get("items"))).isEqualTo(json("""
        [{"id": 12, "sku": "a%b_c", "qty": 3}, {"id": 10, "sku": "MUG", "qty": 2}]"""));
    assertThat(json(rows.get(1).get("items"))).isEqualTo(json("[]"));
  }

  @Test
  void includesOneRow() throws SQLException {
    Query product = products.query().on("sku", "sku").columns("name");

    List<Map<String, Object>> rows = rows(items.query().where(in("id", List.of(10, 12))).columns("id")
        .includeOne("product", product).orderBy("id"));

    assertThat(json(rows.get(0).get("product"))).isEqualTo(json("{\"name\": \"Mug\"}"));
    assertThat(rows.get(1).get("product")).isNull();
  }

  @Test
  void nestsIncludes() throws SQLException {
    Query lines = items.query().on("order_id", "id").columns("sku").orderBy("id")
        .includeOne("product", products.query().on("sku", "sku").columns("name"));

    Map<String, Object> row = rows(orders.query().where(eq("id", 1)).columns("id").include("items", lines))
        .getFirst();

    assertThat(json(row.get("items"))).isEqualTo(json("""
        [{"sku": "MUG", "product": {"name": "Mug"}},
         {"sku": "CUP", "product": {"name": "Cup"}},
         {"sku": "a%b_c", "product": null}]"""));
  }

  @Test
  void includesRowsOfTheSameTable() throws SQLException {
    Query sameRegion = orders.query().on("region", "region").columns("id").orderBy("id");

    Map<String, Object> row = rows(orders.query().where(eq("id", 2)).columns("id").include("neighbours", sameRegion))
        .getFirst();

    assertThat(json(row.get("neighbours"))).isEqualTo(json("[{\"id\": 2}, {\"id\": 5}]"));
  }

  @Test
  void countsRelatedRows() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id")
        .includeCount("item_count", items.query().on("order_id", "id")).orderBy("id"));

    assertThat(rows).extracting(row -> number(row.get("item_count"))).containsExactly(3L, 1L, 1L, 0L, 1L);
  }

  @Test
  void includesWholeRowsWhereSupported() throws SQLException {
    Query all = orders.query().where(eq("id", 2)).columns("id").include("items", items.query().on("order_id", "id"));

    if (!database.includesWholeRows()) {
      assertThatThrownBy(all::build).isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("columns()");
      return;
    }
    assertThat(json(rows(all).getFirst().get("items")))
        .isEqualTo(json("[{\"id\": 13, \"order_id\": 2, \"sku\": \"MUG\", \"qty\": 5}]"));
  }

  @Test
  void filtersByRelatedRows() throws SQLException {
    assertThat(ids(orders.query().where(exists(items.query().on("order_id", "id").where(gte("qty", 4))))))
        .containsExactly(2, 5);
    assertThat(ids(orders.query().where(notExists(items.query().on("order_id", "id"))))).containsExactly(4);
  }

  @Test
  void rejectsOnInATopLevelQuery() {
    assertThatThrownBy(() -> items.query().on("order_id", "id").build()).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("on()");
  }
}
