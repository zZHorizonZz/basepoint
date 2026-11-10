package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Expression.sum;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.aquery.aip160.InvalidFilterException;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class RelationContractTest extends ContractTest {

  private Query itemsWithOrder() {
    return items.query().relation("order", orders.query().on("id", "order_id"));
  }

  private List<Object> values(String column, List<Map<String, Object>> rows) {
    return rows.stream().map(row -> row.get(column)).toList();
  }

  @Test
  @DisplayName("Selects a column of the related row")
  void select() throws SQLException {
    List<Map<String, Object>> rows = rows(itemsWithOrder().columns("id").select("state", column("order", "state"))
        .orderBy("id"));

    assertThat(values("state", rows)).containsExactly("OPEN", "OPEN", "OPEN", "OPEN", "CLOSED", "OPEN");
  }

  @Test
  @DisplayName("Filters and sorts by a column of the related row")
  void filterAndSort() throws SQLException {
    assertThat(idsInOrder(itemsWithOrder().columns("id").where(eq(column("order", "region"), "EU"))
        .orderBy(column("order", "total"), Direction.DESC).orderBy("id"))).containsExactly(14, 10, 11, 12);
  }

  @Test
  @DisplayName("Keeps a row whose related row does not exist, with NULL in its paths")
  void keepMissing() throws SQLException {
    List<Map<String, Object>> rows = rows(items.query().relation("product", products.query().on("sku", "sku"))
        .columns("id").select("name", column("product", "name")).orderBy("id"));

    assertThat(values("name", rows)).containsExactly("Mug", "Cup", null, "Mug", null, "Plate");
  }

  @Test
  @DisplayName("Reads the related row that the filter of the relation keeps, and keeps every row")
  void filterRelation() throws SQLException {
    List<Map<String, Object>> rows = rows(items.query().relation("product", products.query().on("sku", "sku")
        .where(eq("name", "Mug"))).columns("id").select("name", column("product", "name")).orderBy("id"));

    assertThat(values("name", rows)).containsExactly("Mug", null, null, "Mug", null, null);
  }

  @Test
  @DisplayName("Reads a relation of a relation, and joins both")
  void chain() throws SQLException {
    List<Map<String, Object>> rows = rows(itemsWithOrder().relation("archive", archived.query().on("id", "order.id"))
        .columns("id").select("archived", column("archive", "state")).orderBy("id"));

    assertThat(values("archived", rows)).containsExactly("OPEN", "OPEN", "OPEN", null, null, null);
  }

  @Test
  @DisplayName("Groups by a column of the related row")
  void group() throws SQLException {
    List<Map<String, Object>> rows = rows(itemsWithOrder().select("region", column("order", "region"))
        .select("quantity", sum("qty")).groupBy(column("order", "region")).orderBy("region"));

    assertThat(values("region", rows)).containsExactly("EU", "US");
    assertThat(rows).extracting(row -> number(row.get("quantity"))).containsExactly(7L, 9L);
  }

  @Test
  @DisplayName("Reads a relation in a query of exists()")
  void exist() throws SQLException {
    Query plates = items.query().on("order_id", "id").relation("product", products.query().on("sku", "sku"))
        .where(eq(column("product", "name"), "Plate"));

    assertThat(ids(orders.query().columns("id").where(exists(plates)))).containsExactly(5);
  }

  @Test
  @DisplayName("Reads a column of a relation in filter text, and compares it as a column")
  void filterText() throws SQLException {
    assertThat(ids(itemsWithOrder().columns("id").where("order.region = EU AND order.total > 100")))
        .containsExactly(10, 11, 12, 14);
    assertThat(ids(itemsWithOrder().columns("id").where("order.state = \"CL*\""))).containsExactly(14);
  }

  @Test
  @DisplayName("Refuses a relation named after filter text that read its name as a JSON column")
  void refuseLateRelation() {
    Query text = items.query().where("order.region = EU");

    assertThatThrownBy(() -> text.relation("order", orders.query().on("id", "order_id")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Name the relation order before");
  }

  @Test
  @DisplayName("Refuses a test for a key on a column of a relation in filter text")
  void refuseKeyOfPath() {
    assertThatThrownBy(() -> itemsWithOrder().where("order.region:EU")).isInstanceOf(InvalidFilterException.class)
        .hasMessageContaining("supports only ':*'");
  }

  @Test
  @DisplayName("Refuses a relation without on(), a relation of selected rows, and two relations with one name")
  void refuse() {
    assertThatThrownBy(() -> items.query().relation("order", orders.query())).isInstanceOf(
        IllegalArgumentException.class).hasMessageContaining("have no on()");
    assertThatThrownBy(() -> items.query().relation("order", orders.query().on("id", "order_id").columns("id")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only on() and filters");
    assertThatThrownBy(() -> itemsWithOrder().relation("order", orders.query().on("id", "order_id")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("two relations named order");
    assertThatThrownBy(() -> items.query().relation("archive", archived.query().on("id", "order.id")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not name before it");
  }

  @Test
  @DisplayName("Refuses a path to a relation that the query does not name")
  void refuseUnknownPath() {
    assertThatThrownBy(() -> items.query().select("state", column("order", "state")).build())
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("no relation named order");
  }
}
