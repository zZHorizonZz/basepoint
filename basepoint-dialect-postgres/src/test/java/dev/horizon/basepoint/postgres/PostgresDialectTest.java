package dev.horizon.basepoint.postgres;

import static dev.horizon.basepoint.Expression.key;
import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Buildable;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.LockMode;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PostgresDialectTest {

  private static final Dialect DIALECT = new PostgresDialect();
  private static final CollectionReference ORDERS = DIALECT.collection("orders");
  private static final CollectionReference ITEMS = DIALECT.collection("items");

  static Stream<Arguments> statements() {
    return Stream.of(
        arguments("json key", ORDERS.query().where(eq(key("labels", "env"), "prod")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE ((\"t0\".\"labels\" ->> CAST($1 AS text)) = $2)"),
        arguments("dollar placeholders", ORDERS.query().where(eq("state", "OPEN")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (\"t0\".\"state\" = $1)"),
        arguments("quotes in names", DIALECT.collection("a\"b").query(),
            "SELECT \"t0\".* FROM \"a\"\"b\" AS \"t0\""),
        arguments("share lock", ORDERS.query().lock(LockMode.SHARE),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" FOR SHARE"),
        arguments("nowait lock", ORDERS.query().limit(1).lock(LockMode.UPDATE_NOWAIT),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" LIMIT 1 FOR UPDATE NOWAIT"),
        arguments("offset without limit", ORDERS.query().offset(5),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" OFFSET 5"),
        arguments("returning", ORDERS.delete().where(eq("id", 1)).returning(),
            "DELETE FROM \"orders\" WHERE (\"orders\".\"id\" = $1) RETURNING *"),
        arguments("upsert", ORDERS.insert(Map.of("id", 1)).ignore("id"),
            "INSERT INTO \"orders\" (\"id\") VALUES ($1) ON CONFLICT (\"id\") DO NOTHING"),
        arguments("include", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").orderBy("sku")),
            "SELECT \"t0\".\"id\", (SELECT COALESCE(jsonb_agg(to_jsonb(\"t1\") - 'bp_n' ORDER BY \"t1\".\"bp_n\"),"
                + " '[]'::jsonb) FROM (SELECT \"t2\".*, ROW_NUMBER() OVER (ORDER BY \"t2\".\"sku\" ASC) AS \"bp_n\""
                + " FROM \"items\" AS \"t2\" WHERE (\"t2\".\"order_id\" = \"t0\".\"id\") ORDER BY \"t2\".\"sku\" ASC)"
                + " AS \"t1\") AS \"items\" FROM \"orders\" AS \"t0\""),
        arguments("include without order", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id")),
            "SELECT \"t0\".\"id\", (SELECT COALESCE(jsonb_agg(to_jsonb(\"t1\")), '[]'::jsonb) FROM (SELECT \"t2\".*"
                + " FROM \"items\" AS \"t2\" WHERE (\"t2\".\"order_id\" = \"t0\".\"id\")) AS \"t1\") AS \"items\""
                + " FROM \"orders\" AS \"t0\""),
        arguments("include one", ORDERS.query().columns("id").includeOne("item",
            ITEMS.query().on("order_id", "id").columns("sku")),
            "SELECT \"t0\".\"id\", (SELECT to_jsonb(\"t1\") FROM (SELECT \"t2\".\"sku\" FROM \"items\" AS \"t2\""
                + " WHERE (\"t2\".\"order_id\" = \"t0\".\"id\") LIMIT 1) AS \"t1\") AS \"item\" FROM \"orders\" AS \"t0\""));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statements")
  void renders(String name, Buildable builder, String text) {
    assertThat(builder.build().text()).isEqualTo(text);
  }
}
