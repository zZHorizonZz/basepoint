package dev.horizon.basepoint;

import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Expression.key;
import static dev.horizon.basepoint.Expression.sum;
import static dev.horizon.basepoint.Filter.and;
import static dev.horizon.basepoint.Filter.contains;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.exists;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.in;
import static dev.horizon.basepoint.Filter.nin;
import static dev.horizon.basepoint.Filter.or;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RenderingTest {

  private static final Dialect DIALECT = new StandardDialect();
  private static final CollectionReference ORDERS = DIALECT.collection("orders");
  private static final CollectionReference ITEMS = DIALECT.collection("items");

  static Stream<Arguments> statements() {
    return Stream.of(
        arguments("all rows", ORDERS.query(),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\"", List.of()),
        arguments("schema-qualified table", DIALECT.collection("shop.orders").query(),
            "SELECT \"t0\".* FROM \"shop\".\"orders\" AS \"t0\"", List.of()),
        arguments("lock", ORDERS.query().where(eq("id", 1)).lock(LockMode.UPDATE),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (\"t0\".\"id\" = ?) FOR UPDATE", List.of(1)),
        arguments("columns", ORDERS.query().columns("id", "state"),
            "SELECT \"t0\".\"id\", \"t0\".\"state\" FROM \"orders\" AS \"t0\"", List.of()),
        arguments("filters of two calls", ORDERS.query().where(eq("state", "OPEN")).where(gt("total", 100)),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE ((\"t0\".\"state\" = ?) AND (\"t0\".\"total\" > ?))",
            List.of("OPEN", 100)),
        arguments("or", ORDERS.query().where(or(eq("a", 1), eq("b", 2))),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE ((\"t0\".\"a\" = ?) OR (\"t0\".\"b\" = ?))",
            List.of(1, 2)),
        arguments("in", ORDERS.query().where(in("id", List.of(1, 2))),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (\"t0\".\"id\" IN (?, ?))", List.of(1, 2)),
        arguments("in empty", ORDERS.query().where(in("id", List.of())),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (1 = 0)", List.of()),
        arguments("nin empty", ORDERS.query().where(nin("id", List.of())),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (1 = 1)", List.of()),
        arguments("contains", ORDERS.query().where(contains("note", "50%_")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (\"t0\".\"note\" LIKE ? ESCAPE '!')",
            List.of("%50!%!_%")),
        arguments("column to column", ITEMS.query().where(gt("qty", column("min_qty"))),
            "SELECT \"t0\".* FROM \"items\" AS \"t0\" WHERE (\"t0\".\"qty\" > \"t0\".\"min_qty\")", List.of()),
        arguments("json key", ORDERS.query().where(eq(key("labels", "env"), "prod")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (JSON_VALUE(\"t0\".\"labels\", ?) = ?)",
            List.of("$.\"env\"", "prod")),
        arguments("exists", ORDERS.query().where(exists(ITEMS.query().on("order_id", "id"))),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE EXISTS (SELECT 1 FROM \"items\" AS \"t1\""
                + " WHERE (\"t1\".\"order_id\" = \"t0\".\"id\"))",
            List.of()),
        arguments("aggregate", ORDERS.query().columns("state").select("revenue", sum("total")).groupBy("state")
            .having(gt(sum("total"), 1000)).orderBy("revenue", Direction.DESC),
            "SELECT \"t0\".\"state\", SUM(\"t0\".\"total\") AS \"revenue\" FROM \"orders\" AS \"t0\""
                + " GROUP BY \"t0\".\"state\" HAVING (SUM(\"t0\".\"total\") > ?) ORDER BY \"revenue\" DESC",
            List.of(1000)),
        arguments("include count", ORDERS.query().columns("id").includeCount("n", ITEMS.query().on("order_id", "id")),
            "SELECT \"t0\".\"id\", (SELECT COUNT(*) FROM \"items\" AS \"t1\" WHERE (\"t1\".\"order_id\" = \"t0\".\"id\"))"
                + " AS \"n\" FROM \"orders\" AS \"t0\"",
            List.of()),
        arguments("paging", ORDERS.query().orderBy("id").limit(10).offset(20),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" ORDER BY \"t0\".\"id\" ASC LIMIT 10 OFFSET 20", List.of()),
        arguments("distinct", ORDERS.query().columns("state").distinct(),
            "SELECT DISTINCT \"t0\".\"state\" FROM \"orders\" AS \"t0\"", List.of()),
        arguments("union", ORDERS.query().columns("id").union(ITEMS.query().columns("id")),
            "SELECT \"t0\".\"id\" FROM \"orders\" AS \"t0\" UNION ALL SELECT \"t1\".\"id\" FROM \"items\" AS \"t1\"",
            List.of()),
        arguments("sorted union", ORDERS.query().columns("id").union(ITEMS.query().columns("id")).distinct()
            .orderBy("id").limit(5),
            "SELECT \"t0\".* FROM (SELECT \"t1\".\"id\" FROM \"orders\" AS \"t1\" UNION SELECT \"t2\".\"id\""
                + " FROM \"items\" AS \"t2\") AS \"t0\" ORDER BY \"t0\".\"id\" ASC LIMIT 5",
            List.of()),
        arguments("union operand with a limit", ORDERS.query().columns("id").orderBy("id").limit(2)
            .union(ITEMS.query().columns("id")),
            "SELECT \"t0\".* FROM (SELECT \"t1\".\"id\" FROM \"orders\" AS \"t1\" ORDER BY \"t1\".\"id\" ASC LIMIT 2)"
                + " AS \"t0\" UNION ALL SELECT \"t2\".\"id\" FROM \"items\" AS \"t2\"",
            List.of()),
        arguments("union operand drops its order without a limit",
            ORDERS.query().columns("id").orderBy("id").union(ITEMS.query().columns("id")),
            "SELECT \"t0\".\"id\" FROM \"orders\" AS \"t0\" UNION ALL SELECT \"t1\".\"id\" FROM \"items\" AS \"t1\"",
            List.of()),
        arguments("insert", ORDERS.insert(row(1, "OPEN")),
            "INSERT INTO \"orders\" (\"id\", \"state\") VALUES (?, ?)", List.of(1, "OPEN")),
        arguments("insert rows", ORDERS.insert(List.of(row(1, "OPEN"), row(2, "CLOSED"))),
            "INSERT INTO \"orders\" (\"id\", \"state\") VALUES (?, ?), (?, ?)", List.of(1, "OPEN", 2, "CLOSED")),
        arguments("insert defaults", ORDERS.insert(Map.of()),
            "INSERT INTO \"orders\" DEFAULT VALUES", List.of()),
        arguments("replace", ORDERS.insert(row(1, "OPEN")).replace("id").returning("id"),
            "INSERT INTO \"orders\" (\"id\", \"state\") VALUES (?, ?) ON CONFLICT (\"id\")"
                + " DO UPDATE SET \"state\" = EXCLUDED.\"state\" RETURNING \"id\"",
            List.of(1, "OPEN")),
        arguments("ignore", ORDERS.insert(row(1, "OPEN")).ignore("id"),
            "INSERT INTO \"orders\" (\"id\", \"state\") VALUES (?, ?) ON CONFLICT (\"id\") DO NOTHING",
            List.of(1, "OPEN")),
        arguments("update", ORDERS.update().set("state", "CLOSED").increment("total", 5).unset("note")
            .where(eq("id", 1)).returning(),
            "UPDATE \"orders\" SET \"state\" = ?, \"total\" = (\"orders\".\"total\" + ?), \"note\" = NULL"
                + " WHERE (\"orders\".\"id\" = ?) RETURNING *",
            List.of("CLOSED", 5, 1)),
        arguments("update all", ORDERS.update().set("note", "x").all(),
            "UPDATE \"orders\" SET \"note\" = ?", List.of("x")),
        arguments("delete", ORDERS.delete().where(and(eq("state", "CANCELLED"), gt("total", 0))),
            "DELETE FROM \"orders\" WHERE ((\"orders\".\"state\" = ?) AND (\"orders\".\"total\" > ?))",
            List.of("CANCELLED", 0)),
        arguments("delete all", ORDERS.delete().all(),
            "DELETE FROM \"orders\"", List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statements")
  void renders(String name, Buildable builder, String text, List<Object> params) {
    RenderedStatement statement = builder.build();

    assertThat(statement.text()).isEqualTo(text);
    assertThat(statement.params()).containsExactlyElementsOf(params);
  }

  @Test
  void bindsNullValues() {
    RenderedStatement statement = ORDERS.insert(row(1, null)).build();

    assertThat(statement.params()).containsExactly(1, null);
  }

  @Test
  void rejectsIncludesWithoutADialectDecorator() {
    Query query = ORDERS.query().include("items", ITEMS.query().on("order_id", "id").columns("id"));

    assertThatThrownBy(query::build).isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("does not render includes");
  }

  @Test
  void statementReturnsTheTreeBeforeRendering() {
    Statement statement = ORDERS.query().where(eq("id", 1)).limit(1).statement();

    assertThat(statement.clauses()).extracting(Object::getClass).containsExactly(Clause.Select.class,
        Clause.From.class, Clause.Where.class, Clause.Limit.class);
  }

  private static Map<String, Object> row(int id, String state) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("state", state);
    return row;
  }
}
