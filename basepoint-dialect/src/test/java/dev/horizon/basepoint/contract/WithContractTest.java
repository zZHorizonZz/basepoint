package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.add;
import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Expression.value;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.isNull;
import static dev.horizon.basepoint.Filter.lt;
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

  private Query start(CollectionReference categories) {
    return categories.query().columns("id", "parent_id").select("depth", value(0)).where(eq("id", 1));
  }

  private Query step(CollectionReference categories) {
    return categories.query().relation("parent", dialect.collection("tree").query().on("id", "parent_id"))
        .columns("id", "parent_id").select("depth", add(column("parent", "depth"), value(1)));
  }

  @Test
  @DisplayName("Reads every descendant of a row, with its depth")
  void recurse() throws SQLException {
    CollectionReference categories = dialect.collection("categories");
    List<Map<String, Object>> rows = rows(dialect.collection("tree").query().withRecursive("tree", start(categories),
        step(categories)).columns("id", "depth").orderBy("id"));

    assertThat(rows).extracting(row -> (int) number(row.get("id"))).containsExactly(1, 2, 3, 4, 5);
    assertThat(rows).extracting(row -> number(row.get("depth"))).containsExactly(0L, 1L, 1L, 2L, 3L);
  }

  @Test
  @DisplayName("Stops at the depth that the step allows")
  void limitDepth() throws SQLException {
    CollectionReference categories = dialect.collection("categories");
    Query step = step(categories).where(lt(column("parent", "depth"), 1));

    assertThat(ids(dialect.collection("tree").query().withRecursive("tree", start(categories), step).columns("id")))
        .containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("Reads every ancestor of a row")
  void ascend() throws SQLException {
    CollectionReference categories = dialect.collection("categories");
    Query start = categories.query().columns("id", "parent_id").where(eq("id", 5));
    Query step = categories.query().relation("child", dialect.collection("tree").query().on("parent_id", "id"))
        .columns("id", "parent_id");

    assertThat(ids(dialect.collection("tree").query().withRecursive("tree", start, step).columns("id")))
        .containsExactly(1, 2, 4, 5);
  }

  @Test
  @DisplayName("Reads a recursive WITH query after another WITH query")
  void recurseAfterAnother() throws SQLException {
    CollectionReference categories = dialect.collection("categories");
    Query start = dialect.collection("roots").query().columns("id", "parent_id").select("depth", value(0));
    Query query = dialect.collection("tree").query()
        .with("roots", categories.query().columns("id", "parent_id").where(isNull("parent_id")))
        .withRecursive("tree", start, step(categories)).count();

    assertThat(number(rows(query).getFirst().get("count"))).isEqualTo(7);
  }

  @Test
  @DisplayName("Refuses a start that reads the WITH query, a step that does not, and a part with on()")
  void refuseRecursion() {
    CollectionReference categories = dialect.collection("categories");
    CollectionReference tree = dialect.collection("tree");
    assertThatThrownBy(() -> tree.query().withRecursive("tree", step(categories), step(categories)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The start of the recursive WITH query");
    assertThatThrownBy(() -> tree.query().withRecursive("tree", start(categories), start(categories)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not read it");
    assertThatThrownBy(() -> tree.query().withRecursive("tree", start(categories).on("id", "parent_id"),
        step(categories))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("has on()");
  }
}
