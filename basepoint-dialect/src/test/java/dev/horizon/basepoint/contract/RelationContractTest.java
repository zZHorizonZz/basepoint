package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.sum;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public abstract class RelationContractTest extends ContractTest {

  private Query itemsWithOrder() {
    return items.query().relation("order", orders.query().on("id", "order_id"));
  }

  @Test
  @DisplayName("Selects a column of the related row")
  void select() throws SQLException {
    assertThat(valuesOf("state", rows(itemsWithOrder().columns("id").select("state", column("order", "state"))
        .orderBy("id")))).containsExactly("OPEN", "OPEN", "OPEN", "OPEN", "CLOSED", "OPEN");
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
    assertThat(valuesOf("name", rows(items.query().relation("product", products.query().on("sku", "sku"))
        .columns("id").select("name", column("product", "name")).orderBy("id"))))
        .containsExactly("Mug", "Cup", null, "Mug", null, "Plate");
  }

  @Test
  @DisplayName("Reads the related row that the filter of the relation keeps, and keeps every row")
  void filterRelation() throws SQLException {
    assertThat(valuesOf("name", rows(items.query().relation("product", products.query().on("sku", "sku")
        .where(eq("name", "Mug"))).columns("id").select("name", column("product", "name")).orderBy("id"))))
        .containsExactly("Mug", null, null, "Mug", null, null);
  }

  @Test
  @DisplayName("Reads a relation of a relation")
  void chain() throws SQLException {
    assertThat(valuesOf("archived", rows(itemsWithOrder().relation("archive", archived.query().on("id", "order.id"))
        .columns("id").select("archived", column("archive", "state")).orderBy("id"))))
        .containsExactly("OPEN", "OPEN", "OPEN", null, null, null);
  }

  @Test
  @DisplayName("Reads a column of an earlier relation in the filter of a relation")
  void filterByEarlier() throws SQLException {
    assertThat(valuesOf("state", rows(itemsWithOrder().relation("european", orders.query().on("id", "order.id")
        .where(eq(column("order", "region"), "EU"))).columns("id").select("state", column("european", "state"))
        .orderBy("id")))).containsExactly("OPEN", "OPEN", "OPEN", null, "CLOSED", null);
  }

  @Test
  @DisplayName("Reads a column of a relation of the enclosing query in the query of exists()")
  void readEnclosing() throws SQLException {
    assertThat(ids(itemsWithOrder().columns("id").where(exists(products.query().on("sku", "sku")
        .where(eq(column("order", "region"), "EU")))))).containsExactly(10, 11);
  }

  @Test
  @DisplayName("Groups by a column of the related row")
  void group() throws SQLException {
    List<Map<String, Object>> rows = rows(itemsWithOrder().select("region", column("order", "region"))
        .select("quantity", sum("qty")).groupBy(column("order", "region")).orderBy("region"));

    assertThat(valuesOf("region", rows)).containsExactly("EU", "US");
    assertThat(rows).extracting(row -> number(row.get("quantity"))).containsExactly(7L, 9L);
  }

  @Test
  @DisplayName("Reads a relation in the query of exists()")
  void exist() throws SQLException {
    Query plates = items.query().on("order_id", "id").relation("product", products.query().on("sku", "sku"))
        .where(eq(column("product", "name"), "Plate"));

    assertThat(ids(orders.query().columns("id").where(exists(plates)))).containsExactly(5);
  }

  @Test
  @DisplayName("Reads a relation in an include")
  void include() throws SQLException {
    Query lines = items.query().on("order_id", "id").relation("product", products.query().on("sku", "sku"))
        .columns("sku").select("name", column("product", "name")).orderBy("id");

    assertThat(json(rows(orders.query().columns("id").include("lines", lines).where(eq("id", 2))).getFirst()
        .get("lines"))).isEqualTo(json("[{\"sku\": \"MUG\", \"name\": \"Mug\"}]"));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = ';', textBlock = """
      order.region = EU AND order.total > 100 ; 10 11 12 14
      order.state = "CL*"                     ; 14
      NOT order.region = EU                   ; 13 15
      order.note:*                            ; 13 14
      """)
  @DisplayName("Reads a column of a relation in filter text")
  void filterText(String text, String expected) throws SQLException {
    assertThat(ids(itemsWithOrder().columns("id").where(text))).containsExactlyElementsOf(List.of(expected.split(" "))
        .stream().map(Integer::valueOf).toList());
  }

  @Test
  @DisplayName("Reads a column of a relation in filter text written before the relation")
  void filterTextFirst() throws SQLException {
    assertThat(ids(items.query().columns("id").where("order.region = EU").relation("order", orders.query().on("id",
        "order_id")))).containsExactly(10, 11, 12, 14);
  }

  @Test
  @DisplayName("Refuses a test for a key on a column of a relation in filter text")
  void refuseKeyOfPath() {
    assertThatThrownBy(() -> itemsWithOrder().where("order.region:EU").build())
        .isInstanceOf(InvalidFilterException.class).hasMessageContaining("supports only ':*'");
  }
}
