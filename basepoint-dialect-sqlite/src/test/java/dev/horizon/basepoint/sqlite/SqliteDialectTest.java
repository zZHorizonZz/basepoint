package dev.horizon.basepoint.sqlite;

import static dev.horizon.basepoint.Expression.key;
import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Buildable;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.LockMode;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SqliteDialectTest {

  private static final Dialect DIALECT = new SqliteDialect();
  private static final CollectionReference ORDERS = DIALECT.collection("orders");
  private static final CollectionReference ITEMS = DIALECT.collection("items");

  static Stream<Arguments> statements() {
    return Stream.of(
        arguments("json key", ORDERS.query().where(eq(key("labels", "env"), "prod")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (CAST(json_extract(\"t0\".\"labels\", ?) AS TEXT) = ?)"),
        arguments("question marks", ORDERS.query().where(eq("state", "OPEN")),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" WHERE (\"t0\".\"state\" = ?)"),
        arguments("no lock", ORDERS.query().lock(LockMode.UPDATE),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\""),
        arguments("offset without limit", ORDERS.query().offset(5),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" LIMIT -1 OFFSET 5"),
        arguments("offset with limit", ORDERS.query().limit(10).offset(5),
            "SELECT \"t0\".* FROM \"orders\" AS \"t0\" LIMIT 10 OFFSET 5"),
        arguments("returning", ORDERS.update().set("state", "CLOSED").where(eq("id", 1)).returning("id"),
            "UPDATE \"orders\" SET \"state\" = ? WHERE (\"orders\".\"id\" = ?) RETURNING \"id\""),
        arguments("include", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").columns("sku").orderBy("sku")),
            "SELECT \"t0\".\"id\", (SELECT COALESCE(json_group_array(json_object('sku', \"t1\".\"sku\") ORDER BY"
                + " \"t1\".\"bp_n\"), '[]') FROM (SELECT \"t2\".\"sku\", ROW_NUMBER() OVER (ORDER BY \"t2\".\"sku\" ASC)"
                + " AS \"bp_n\" FROM \"items\" AS \"t2\" WHERE (\"t2\".\"order_id\" = \"t0\".\"id\") ORDER BY"
                + " \"t2\".\"sku\" ASC) AS \"t1\") AS \"items\" FROM \"orders\" AS \"t0\""),
        arguments("nested include", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").columns("sku")
                .includeCount("n", DIALECT.collection("parts").query().on("sku", "sku"))
                .includeOne("product", DIALECT.collection("products").query().on("sku", "sku").columns("name"))),
            "SELECT \"t0\".\"id\", (SELECT COALESCE(json_group_array(json_object('sku', \"t1\".\"sku\", 'n',"
                + " \"t1\".\"n\", 'product', json(\"t1\".\"product\"))), '[]') FROM (SELECT \"t2\".\"sku\", (SELECT"
                + " COUNT(*) FROM \"parts\" AS \"t3\" WHERE (\"t3\".\"sku\" = \"t2\".\"sku\")) AS \"n\", (SELECT"
                + " json_object('name', \"t4\".\"name\") FROM (SELECT \"t5\".\"name\" FROM \"products\" AS \"t5\" WHERE"
                + " (\"t5\".\"sku\" = \"t2\".\"sku\") LIMIT 1) AS \"t4\") AS \"product\" FROM \"items\" AS \"t2\" WHERE"
                + " (\"t2\".\"order_id\" = \"t0\".\"id\")) AS \"t1\") AS \"items\" FROM \"orders\" AS \"t0\""));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statements")
  void renders(String name, Buildable builder, String text) {
    assertThat(builder.build().text()).isEqualTo(text);
  }

  @Test
  void rejectsIncludesWithoutColumns() {
    assertThatThrownBy(() -> ORDERS.query().include("items", ITEMS.query().on("order_id", "id")).build())
        .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("columns()");
  }
}
