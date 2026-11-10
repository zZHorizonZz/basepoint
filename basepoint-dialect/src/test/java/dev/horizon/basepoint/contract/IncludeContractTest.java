package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.max;
import static dev.horizon.basepoint.expression.Expression.subquery;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
import static dev.horizon.basepoint.filter.Filter.gt;
import static dev.horizon.basepoint.filter.Filter.gte;
import static dev.horizon.basepoint.filter.Filter.in;
import static dev.horizon.basepoint.filter.Filter.notExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.UnsupportedFeatureException;
import dev.horizon.basepoint.expression.Expression;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class IncludeContractTest extends ContractTest {

  @Test
  @DisplayName("Includes the related rows as a JSON array, in their order and up to their limit")
  void includeMany() throws SQLException {
    Query lines = items.query().on("order_id", "id").columns("id", "sku", "qty").orderBy("qty", Direction.DESC)
        .limit(2);
    List<Map<String, Object>> rows = rows(orders.query().where(in("id", List.of(1, 4))).columns("id")
        .include("items", lines).orderBy("id"));

    assertThat(json(rows.get(0).get("items"))).isEqualTo(json("""
        [{"id": 12, "sku": "a%b_c", "qty": 3}, {"id": 10, "sku": "MUG", "qty": 2}]"""));
    assertThat(json(rows.get(1).get("items"))).isEqualTo(json("[]"));
  }

  @Test
  @DisplayName("Includes the related rows in their order by a selected value")
  void includeByValue() throws SQLException {
    Query lines = items.query().on("order_id", "id").columns("sku").select("double", Expression.multiply(Expression
        .column("qty"), Expression.value(2))).orderBy("double", Direction.DESC);
    Map<String, Object> row = rows(orders.query().where(eq("id", 1)).columns("id").include("items", lines))
        .getFirst();

    assertThat(json(row.get("items"))).isEqualTo(json("""
        [{"sku": "a%b_c", "double": 6}, {"sku": "MUG", "double": 4}, {"sku": "CUP", "double": 2}]"""));
  }

  @Test
  @DisplayName("Includes the first related row as a JSON object, and NULL without one")
  void includeOne() throws SQLException {
    Query product = products.query().on("sku", "sku").columns("name");
    List<Map<String, Object>> rows = rows(items.query().where(in("id", List.of(10, 12))).columns("id")
        .includeOne("product", product).orderBy("id"));

    assertThat(json(rows.get(0).get("product"))).isEqualTo(json("{\"name\": \"Mug\"}"));
    assertThat(rows.get(1).get("product")).isNull();
  }

  @Test
  @DisplayName("Nests an include in an include")
  void nest() throws SQLException {
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
  @DisplayName("Includes the rows of the same table")
  void includeSameTable() throws SQLException {
    Query sameRegion = orders.query().on("region", "region").columns("id").orderBy("id");
    Map<String, Object> row = rows(orders.query().where(eq("id", 2)).columns("id").include("neighbours", sameRegion))
        .getFirst();

    assertThat(json(row.get("neighbours"))).isEqualTo(json("[{\"id\": 2}, {\"id\": 5}]"));
  }

  @Test
  @DisplayName("Includes every column of the related rows where the database lists them itself")
  void includeWholeRows() throws SQLException {
    assumeTrue(database.includesWholeRows());
    Query all = orders.query().where(eq("id", 2)).columns("id").include("items", items.query().on("order_id", "id"));

    assertThat(json(rows(all).getFirst().get("items")))
        .isEqualTo(json("[{\"id\": 13, \"order_id\": 2, \"sku\": \"MUG\", \"qty\": 5}]"));
  }

  @Test
  @DisplayName("Refuses to include every column where the database needs their names")
  void refuseWholeRows() {
    assumeFalse(database.includesWholeRows());
    Query all = orders.query().where(eq("id", 2)).columns("id").include("items", items.query().on("order_id", "id"));

    assertThatThrownBy(all::build).isInstanceOf(UnsupportedFeatureException.class).hasMessageContaining("columns()");
  }

  @Test
  @DisplayName("Selects the number of related rows")
  void countRelated() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("item_count", subquery(items.query()
        .on("order_id", "id").count())).orderBy("id"));

    assertThat(rows).extracting(row -> number(row.get("item_count"))).containsExactly(3L, 1L, 1L, 0L, 1L);
  }

  @Test
  @DisplayName("Filters by the value of a subquery")
  void filterBySubquery() throws SQLException {
    Expression itemCount = subquery(items.query().on("order_id", "id").count());

    assertThat(ids(orders.query().columns("id").where(gt(itemCount, 1)))).containsExactly(1);
  }

  @Test
  @DisplayName("Selects and sorts by an aggregate of related rows")
  void sortBySubquery() throws SQLException {
    Expression largest = subquery(items.query().on("order_id", "id").select("largest", max("qty")));
    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("largest", largest)
        .where(gt(largest, 1)).orderBy(largest, Direction.DESC));

    assertThat(idsOf(rows)).containsExactly(2, 5, 1);
    assertThat(rows).extracting(row -> number(row.get("largest"))).containsExactly(5L, 4L, 3L);
  }

  @Test
  @DisplayName("Filters by whether related rows exist")
  void filterByExistence() throws SQLException {
    assertThat(ids(orders.query().where(exists(items.query().on("order_id", "id").where(gte("qty", 4))))))
        .containsExactly(2, 5);
    assertThat(ids(orders.query().where(notExists(items.query().on("order_id", "id"))))).containsExactly(4);
  }

  @Test
  @DisplayName("Filters by related rows that the relation filters by text")
  void filterByText() throws SQLException {
    assertThat(ids(orders.query().where(exists(items.query().on("order_id", "id").where("sku = \"M*\"")))))
        .containsExactly(1, 2);
  }
}
