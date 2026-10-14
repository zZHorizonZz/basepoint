package dev.horizon.basepoint;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValidationTest {

  private static final CollectionReference ORDERS = new StandardDialect().collection("orders");

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments("limit 0", call(() -> ORDERS.query().limit(0)), IllegalArgumentException.class,
            "The limit must be greater than 0"),
        arguments("negative offset", call(() -> ORDERS.query().offset(-1)), IllegalArgumentException.class,
            "The offset must be 0 or greater"),
        arguments("compound limit 0", call(() -> ORDERS.query().union(ORDERS.query()).limit(0)),
            IllegalArgumentException.class, "The limit must be greater than 0"),
        arguments("insert without rows", call(() -> ORDERS.insert(List.of())), IllegalArgumentException.class,
            "The insert has no rows."),
        arguments("insert rows with other columns",
            call(() -> ORDERS.insert(List.of(Map.of("id", 1), Map.of("state", "OPEN")))),
            IllegalArgumentException.class, "Every row must set the same columns."),
        arguments("insert two default rows", call(() -> ORDERS.insert(List.of(Map.of(), Map.of()))),
            IllegalArgumentException.class, "Only one row can use the default value of every column."),
        arguments("upsert without key", call(() -> ORDERS.insert(Map.of("id", 1)).replace()),
            IllegalArgumentException.class, "The key has no columns."),
        arguments("upsert of a default row", call(() -> ORDERS.insert(Map.of()).ignore("id")),
            IllegalArgumentException.class, "cannot use a key"),
        arguments("update without columns", call(() -> ORDERS.update().where(eq("id", 1)).build()),
            IllegalStateException.class, "The update sets no columns."),
        arguments("update without filter", call(() -> ORDERS.update().set("state", "OPEN").build()),
            IllegalStateException.class, "The update has no filter."),
        arguments("delete without filter", call(() -> ORDERS.delete().build()), IllegalStateException.class,
            "The delete has no filter."),
        arguments("on in a top-level query", call(() -> ORDERS.query().on("id", "id").build()),
            IllegalStateException.class, "Use on() only in a query"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalid")
  void rejects(String name, ThrowingCallable call, Class<? extends Throwable> type, String message) {
    assertThatThrownBy(call).isInstanceOf(type).hasMessageContaining(message);
  }

  private static ThrowingCallable call(ThrowingCallable call) {
    return call;
  }
}
