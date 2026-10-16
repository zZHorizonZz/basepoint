package dev.horizon.basepoint.mariadb;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Buildable;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.LockMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MariaDbDialectTest {

  private static final Dialect DIALECT = new MariaDbDialect();
  private static final CollectionReference ORDERS = DIALECT.collection("orders");
  private static final CollectionReference ITEMS = DIALECT.collection("items");

  static Stream<Arguments> statements() {
    return Stream.of(
        arguments("backticks and question marks", ORDERS.query().where(eq("state", "OPEN")),
            "SELECT `t0`.* FROM `orders` AS `t0` WHERE (`t0`.`state` = ?)"),
        arguments("backticks in names", DIALECT.collection("a`b").query(),
            "SELECT `t0`.* FROM `a``b` AS `t0`"),
        arguments("share lock", ORDERS.query().lock(LockMode.SHARE),
            "SELECT `t0`.* FROM `orders` AS `t0` LOCK IN SHARE MODE"),
        arguments("update lock", ORDERS.query().limit(1).lock(LockMode.UPDATE_SKIP_LOCKED),
            "SELECT `t0`.* FROM `orders` AS `t0` LIMIT 1 FOR UPDATE SKIP LOCKED"),
        arguments("offset without limit", ORDERS.query().offset(5),
            "SELECT `t0`.* FROM `orders` AS `t0` LIMIT 18446744073709551615 OFFSET 5"),
        arguments("offset with limit", ORDERS.query().limit(10).offset(5),
            "SELECT `t0`.* FROM `orders` AS `t0` LIMIT 10 OFFSET 5"),
        arguments("returning", ORDERS.update().set("state", "CLOSED").where(eq("id", 1)).returning("id"),
            "UPDATE `orders` SET `state` = ? WHERE (`orders`.`id` = ?) RETURNING `id`"),
        arguments("replace", ORDERS.insert(row()).replace("id").returning("id"),
            "INSERT INTO `orders` (`id`, `state`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `state` = VALUES(`state`)"
                + " RETURNING `id`"),
        arguments("ignore", ORDERS.insert(row()).ignore("id"),
            "INSERT INTO `orders` (`id`, `state`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `id` = `id`"),
        arguments("default row", ORDERS.insert(Map.of()),
            "INSERT INTO `orders` () VALUES ()"),
        arguments("include", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").columns("sku").orderBy("sku").limit(2).offset(1)),
            "SELECT `t0`.`id`, COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('sku', `t1`.`sku`) ORDER BY `t1`.`sku` ASC"
                + " LIMIT 2 OFFSET 1) FROM `items` AS `t1` WHERE (`t1`.`order_id` = `t0`.`id`)), JSON_ARRAY())"
                + " AS `items` FROM `orders` AS `t0`"),
        arguments("include without order", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").columns("sku")),
            "SELECT `t0`.`id`, COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('sku', `t1`.`sku`)) FROM `items` AS `t1`"
                + " WHERE (`t1`.`order_id` = `t0`.`id`)), JSON_ARRAY()) AS `items` FROM `orders` AS `t0`"),
        arguments("include one", ORDERS.query().columns("id").includeOne("item",
            ITEMS.query().on("order_id", "id").columns("sku").orderBy("sku")),
            "SELECT `t0`.`id`, (SELECT JSON_OBJECT('sku', `t1`.`sku`) FROM `items` AS `t1` WHERE"
                + " (`t1`.`order_id` = `t0`.`id`) ORDER BY `t1`.`sku` ASC LIMIT 1) AS `item` FROM `orders` AS `t0`"));
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

  private static Map<String, Object> row() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 1);
    row.put("state", "OPEN");
    return row;
  }
}
