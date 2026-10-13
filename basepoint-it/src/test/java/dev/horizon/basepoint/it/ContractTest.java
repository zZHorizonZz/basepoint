package dev.horizon.basepoint.it;

import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Expression.count;
import static dev.horizon.basepoint.Expression.sum;
import static dev.horizon.basepoint.Filter.and;
import static dev.horizon.basepoint.Filter.between;
import static dev.horizon.basepoint.Filter.contains;
import static dev.horizon.basepoint.Filter.endsWith;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.gte;
import static dev.horizon.basepoint.Filter.in;
import static dev.horizon.basepoint.Filter.like;
import static dev.horizon.basepoint.Filter.lt;
import static dev.horizon.basepoint.Filter.ne;
import static dev.horizon.basepoint.Filter.nin;
import static dev.horizon.basepoint.Filter.not;
import static dev.horizon.basepoint.Filter.notExists;
import static dev.horizon.basepoint.Filter.or;
import static dev.horizon.basepoint.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.horizon.aquery.cel.CelFilterParser;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  private Database database;
  private Connection connection;
  private CollectionReference orders;
  private CollectionReference items;
  private CollectionReference products;
  private CollectionReference archived;
  private CollectionReference defaults;

  protected abstract Database database();

  @BeforeAll
  void connect() throws SQLException {
    database = database();
    connection = database.connect();
    Dialect dialect = database.dialect();
    orders = dialect.collection("orders");
    items = dialect.collection("items");
    products = dialect.collection("products");
    archived = dialect.collection("archived");
    defaults = dialect.collection("defaults");
  }

  @AfterAll
  void close() throws SQLException {
    connection.close();
  }

  @BeforeEach
  void tables() throws SQLException {
    for (String table : List.of("items", "orders", "products", "archived", "defaults")) {
      sql("DROP TABLE IF EXISTS " + table);
    }
    sql("CREATE TABLE orders (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL, total INT NOT NULL, region VARCHAR(10),"
        + " note VARCHAR(50))");
    sql("CREATE TABLE items (id INT PRIMARY KEY, order_id INT NOT NULL, sku VARCHAR(20) NOT NULL, qty INT NOT NULL)");
    sql("CREATE TABLE products (sku VARCHAR(20) PRIMARY KEY, name VARCHAR(50) NOT NULL)");
    sql("CREATE TABLE archived (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL)");
    sql("CREATE TABLE defaults (id INT NOT NULL DEFAULT 7, label VARCHAR(10) NOT NULL DEFAULT 'x')");
    sql("INSERT INTO orders (id, state, total, region, note) VALUES (1, 'OPEN', 150, 'EU', NULL),"
        + " (2, 'OPEN', 80, 'US', 'rush'), (3, 'CLOSED', 300, 'EU', 'gift'), (4, 'CANCELLED', 50, NULL, NULL),"
        + " (5, 'OPEN', 220, 'US', NULL)");
    sql("INSERT INTO items (id, order_id, sku, qty) VALUES (10, 1, 'MUG', 2), (11, 1, 'CUP', 1), (12, 1, 'a%b_c', 3),"
        + " (13, 2, 'MUG', 5), (14, 3, 'x[1]', 1), (15, 5, 'PLATE', 4)");
    sql("INSERT INTO products (sku, name) VALUES ('MUG', 'Mug'), ('CUP', 'Cup'), ('PLATE', 'Plate')");
    sql("INSERT INTO archived (id, state) VALUES (1, 'OPEN'), (6, 'CLOSED'), (7, 'OPEN')");
  }

  @Test
  void filtersBuiltInJava() throws SQLException {
    assertThat(ids(orders.query().where(and(eq("state", "OPEN"), gt("total", 100))))).containsExactly(1, 5);
    assertThat(ids(orders.query().where(or(eq("region", "US"), gte("total", 300))))).containsExactly(2, 3, 5);
    assertThat(ids(orders.query().where(not(eq("state", "OPEN"))))).containsExactly(3, 4);
    assertThat(ids(orders.query().where(between("total", 80, 150)))).containsExactly(1, 2);
    assertThat(ids(orders.query().where(ne("state", "OPEN")).where(lt("total", 100)))).containsExactly(4);
  }

  @Test
  void filtersOfText() throws SQLException {
    assertThat(ids(orders.query().where("state = \"OPEN\" AND total > 100"))).containsExactly(1, 5);
    assertThat(ids(orders.query().where("region = EU"))).containsExactly(1, 3);
    assertThat(ids(orders.query().where("note:*"))).containsExactly(2, 3);
    assertThat(ids(orders.query().where("note = null"))).containsExactly(1, 4, 5);
    assertThat(ids(orders.query().where("state = \"C*\""))).containsExactly(3, 4);
    assertThat(ids(orders.query().where("NOT state = OPEN"))).containsExactly(3, 4);
    assertThat(ids(orders.query().where("total >= 220 OR region = US"))).containsExactly(2, 3, 5);
  }

  @Test
  void filtersOfCel() throws SQLException {
    Filter open = Filter.of("state == 'OPEN' && total > 100", new CelFilterParser());

    assertThat(ids(orders.query().where(open))).containsExactly(1, 5);
  }

  @Test
  void nullsAndLists() throws SQLException {
    assertThat(ids(orders.query().where(eq("note", null)))).containsExactly(1, 4, 5);
    assertThat(ids(orders.query().where(ne("note", null)))).containsExactly(2, 3);
    assertThat(ids(orders.query().where(in("region", List.of("EU", "US"))))).containsExactly(1, 2, 3, 5);
    assertThat(ids(orders.query().where(in("region", List.of())))).isEmpty();
    assertThat(ids(orders.query().where(nin("id", List.of())))).containsExactly(1, 2, 3, 4, 5);
    assertThat(ids(orders.query().where(nin("region", List.of("EU"))))).containsExactly(2, 5);
    assertThat(ids(orders.query().where(or()))).isEmpty();
    assertThat(ids(orders.query().where(and()))).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void textsWithWildcards() throws SQLException {
    assertThat(ids(items.query().where(contains("sku", "%b_")))).containsExactly(12);
    assertThat(ids(items.query().where(startsWith("sku", "a%")))).containsExactly(12);
    assertThat(ids(items.query().where(endsWith("sku", "_c")))).containsExactly(12);
    assertThat(ids(items.query().where(contains("sku", "[1]")))).containsExactly(14);
    assertThat(ids(items.query().where(like("sku", "M%")))).containsExactly(10, 13);
  }

  @Test
  void columnsCompared() throws SQLException {
    assertThat(ids(items.query().where(gt("qty", column("order_id"))))).containsExactly(10, 12, 13);
  }

  @Test
  void pages() throws SQLException {
    assertThat(ids(orders.query().orderBy("total", Direction.DESC).limit(2).build(), false)).containsExactly(3, 5);
    assertThat(ids(orders.query().offset(1).limit(2))).containsExactly(2, 3);
    assertThat(ids(orders.query().offset(3))).containsExactly(4, 5);
    assertThat(ids(orders.query().limit(2))).containsExactly(1, 2);
  }

  @Test
  void distinctRows() throws SQLException {
    List<Map<String, Object>> states = rows(orders.query().columns("state").distinct().orderBy("state").build());

    assertThat(states).extracting(row -> row.get("state")).containsExactly("CANCELLED", "CLOSED", "OPEN");
  }

  @Test
  void aggregates() throws SQLException {
    List<Map<String, Object>> groups = rows(orders.query().columns("state").select("orders", count())
        .select("revenue", sum("total")).groupBy("state").having(gt(sum("total"), 100))
        .orderBy("revenue", Direction.DESC).build());

    assertThat(groups).extracting(row -> row.get("state"), row -> number(row.get("orders")),
        row -> number(row.get("revenue")))
        .containsExactly(tuple("OPEN", 3L, 450L),
            tuple("CLOSED", 1L, 300L));
  }

  @Test
  void counts() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().where(eq("state", "OPEN")).count().build());

    assertThat(number(rows.getFirst().get("count"))).isEqualTo(3L);
  }

  @Test
  void includes() throws Exception {
    Query product = products.query().on("sku", "sku").columns("name");
    Query lines = items.query().on("order_id", "id").columns("id", "sku", "qty").orderBy("qty", Direction.DESC)
        .limit(2).includeOne("product", product);

    List<Map<String, Object>> rows = rows(orders.query().where(in("id", List.of(1, 4))).columns("id", "state")
        .include("items", lines).orderBy("id").build());

    assertThat(rows).hasSize(2);
    assertThat(json(rows.get(0).get("items"))).isEqualTo(JSON.readTree("""
        [{"id": 12, "sku": "a%b_c", "qty": 3, "product": null},
         {"id": 10, "sku": "MUG", "qty": 2, "product": {"name": "Mug"}}]"""));
    assertThat(json(rows.get(1).get("items"))).isEqualTo(JSON.readTree("[]"));
  }

  @Test
  void includesOfTheirOwnTable() throws Exception {
    Query sameRegion = orders.query().on("region", "region").columns("id").orderBy("id");

    List<Map<String, Object>> rows = rows(orders.query().where(eq("id", 2)).columns("id")
        .include("neighbours", sameRegion).build());

    assertThat(json(rows.getFirst().get("neighbours"))).isEqualTo(JSON.readTree("[{\"id\": 2}, {\"id\": 5}]"));
  }

  @Test
  void includedCounts() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id")
        .includeCount("item_count", items.query().on("order_id", "id")).orderBy("id").build());

    assertThat(rows).extracting(row -> number(row.get("item_count"))).containsExactly(3L, 1L, 1L, 0L, 1L);
  }

  @Test
  void includesOfWholeRows() throws Exception {
    Query all = orders.query().where(eq("id", 2)).columns("id").include("items", items.query().on("order_id", "id"));

    if (!database.jsonOfRows()) {
      assertThatThrownBy(all::build).isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("columns(...)");
      return;
    }
    assertThat(json(rows(all.build()).getFirst().get("items")))
        .isEqualTo(JSON.readTree("[{\"id\": 13, \"order_id\": 2, \"sku\": \"MUG\", \"qty\": 5}]"));
  }

  @Test
  void relatedRowsExist() throws SQLException {
    assertThat(ids(orders.query().where(exists(items.query().on("order_id", "id").where(gte("qty", 4))))))
        .containsExactly(2, 5);
    assertThat(ids(orders.query().where(notExists(items.query().on("order_id", "id"))))).containsExactly(4);
  }

  @Test
  void setsOfRows() throws SQLException {
    Query open = orders.query().columns("id").where(eq("state", "OPEN"));
    Query archivedOpen = archived.query().columns("id").where(eq("state", "OPEN"));

    assertThat(ids(open.union(archivedOpen).build(), true)).containsExactly(1, 1, 2, 5, 7);
    assertThat(ids(open.union(archivedOpen).distinct().build(), true)).containsExactly(1, 2, 5, 7);
    assertThat(ids(open.intersect(archivedOpen).build(), true)).containsExactly(1);
    assertThat(ids(open.except(archivedOpen).build(), true)).containsExactly(2, 5);
    assertThat(ids(open.union(archivedOpen).orderBy("id", Direction.DESC).limit(3).build(), false))
        .containsExactly(7, 5, 2);
    assertThat(ids(orders.query().columns("id").orderBy("id").limit(2)
        .union(archived.query().columns("id").where(eq("id", 7))).build(), true)).containsExactly(1, 2, 7);
    assertThat(ids(open.union(archivedOpen).except(orders.query().columns("id").where(eq("id", 1))).build(), true))
        .containsExactly(2, 5, 7);
  }

  @Test
  void inserts() throws SQLException {
    assertThat(update(orders.insert(row(6, "OPEN", 10)).build())).isEqualTo(1);
    assertThat(update(orders.insert(List.of(row(7, "OPEN", 20), row(8, "CLOSED", 30))).build())).isEqualTo(2);

    assertThat(ids(orders.query().where(gt("id", 5)))).containsExactly(6, 7, 8);
  }

  @Test
  void insertsGiveTheirRows() throws SQLException {
    var insert = orders.insert(List.of(row(6, "OPEN", 10), row(7, "OPEN", 20))).returning("id", "state");

    if (!database.returning()) {
      assertThatThrownBy(insert::build).isInstanceOf(UnsupportedOperationException.class);
      return;
    }
    assertThat(ids(insert.build(), true)).containsExactly(6, 7);
  }

  @Test
  void insertsOfDefaults() throws SQLException {
    update(defaults.insert(Map.of()).build());

    List<Map<String, Object>> rows = rows(defaults.query().build());
    assertThat(rows).extracting(row -> number(row.get("id")), row -> row.get("label"))
        .containsExactly(tuple(7L, "x"));
  }

  @Test
  void upserts() throws SQLException {
    update(orders.insert(List.of(row(1, "CLOSED", 999), row(6, "OPEN", 10))).replace("id").build());
    update(orders.insert(List.of(row(2, "CLOSED", 999), row(7, "OPEN", 20))).ignore("id").build());

    assertThat(rows(orders.query().where(in("id", List.of(1, 2, 6, 7))).orderBy("id").build()))
        .extracting(row -> number(row.get("id")), row -> row.get("state"), row -> number(row.get("total")))
        .containsExactly(tuple(1L, "CLOSED", 999L),
            tuple(2L, "OPEN", 80L),
            tuple(6L, "OPEN", 10L),
            tuple(7L, "OPEN", 20L));
  }

  @Test
  void upsertsGiveTheirRows() throws SQLException {
    var upsert = orders.insert(List.of(row(1, "CLOSED", 999), row(6, "OPEN", 10))).replace("id").returning("id");

    if (!database.returning()) {
      assertThatThrownBy(upsert::build).isInstanceOf(UnsupportedOperationException.class);
      return;
    }
    assertThat(ids(upsert.build(), true)).containsExactly(1, 6);
  }

  @Test
  void updates() throws SQLException {
    assertThat(update(orders.update().set("state", "CLOSED").increment("total", 5).unset("note")
        .where(eq("id", 2)).build())).isEqualTo(1);

    Map<String, Object> row = rows(orders.query().where(eq("id", 2)).build()).getFirst();
    assertThat(row.get("state")).isEqualTo("CLOSED");
    assertThat(number(row.get("total"))).isEqualTo(85L);
    assertThat(row.get("note")).isNull();
  }

  @Test
  void updatesOfEveryRowNeedAll() throws SQLException {
    assertThatThrownBy(() -> orders.update().set("note", "x").build()).isInstanceOf(IllegalStateException.class);

    assertThat(update(orders.update().set("note", "x").all().build())).isEqualTo(5);
  }

  @Test
  void updatesGiveTheirRows() throws SQLException {
    var update = orders.update().increment("total", 1).where("state = OPEN").returning("id", "total");

    if (!database.returning()) {
      assertThatThrownBy(update::build).isInstanceOf(UnsupportedOperationException.class);
      return;
    }
    assertThat(rows(update.build())).extracting(row -> number(row.get("id")), row -> number(row.get("total")))
        .containsExactlyInAnyOrder(tuple(1L, 151L),
            tuple(2L, 81L), tuple(5L, 221L));
  }

  @Test
  void deletes() throws SQLException {
    assertThat(update(orders.delete().where(notExists(items.query().on("order_id", "id"))).build())).isEqualTo(1);
    assertThatThrownBy(() -> orders.delete().build()).isInstanceOf(IllegalStateException.class);

    assertThat(ids(orders.query())).containsExactly(1, 2, 3, 5);
  }

  @Test
  void deletesGiveTheirRows() throws SQLException {
    var delete = items.delete().where(eq("order_id", 1)).returning("id");

    if (!database.returning()) {
      assertThatThrownBy(delete::build).isInstanceOf(UnsupportedOperationException.class);
      return;
    }
    assertThat(ids(delete.build(), true)).containsExactly(10, 11, 12);
  }

  private static Map<String, Object> row(int id, String state, int total) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("state", state);
    row.put("total", total);
    return row;
  }

  private List<Integer> ids(Query query) throws SQLException {
    return ids(query.orderBy("id").build(), false);
  }

  private List<Integer> ids(Statement statement, boolean sort) throws SQLException {
    List<Integer> ids = new ArrayList<>(rows(statement).stream().map(row -> (int) number(row.get("id"))).toList());
    if (sort) {
      ids.sort(null);
    }
    return ids;
  }

  private static long number(Object value) {
    return ((Number) value).longValue();
  }

  private static JsonNode json(Object value) throws Exception {
    return JSON.readTree(value.toString());
  }

  private List<Map<String, Object>> rows(Statement statement) throws SQLException {
    try (PreparedStatement prepared = prepare(statement)) {
      boolean hasRows = prepared.execute();
      while (!hasRows && prepared.getUpdateCount() != -1) {
        hasRows = prepared.getMoreResults();
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      if (!hasRows) {
        return rows;
      }
      try (ResultSet result = prepared.getResultSet()) {
        ResultSetMetaData meta = result.getMetaData();
        while (result.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i).toLowerCase(), result.getObject(i));
          }
          rows.add(row);
        }
      }
      return rows;
    }
  }

  private int update(Statement statement) throws SQLException {
    try (PreparedStatement prepared = prepare(statement)) {
      return prepared.executeUpdate();
    }
  }

  private PreparedStatement prepare(Statement statement) throws SQLException {
    PreparedStatement prepared = connection.prepareStatement(statement.text());
    for (int i = 0; i < statement.params().size(); i++) {
      prepared.setObject(i + 1, statement.params().get(i));
    }
    return prepared;
  }

  private void sql(String text) throws SQLException {
    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute(text);
    }
  }
}
