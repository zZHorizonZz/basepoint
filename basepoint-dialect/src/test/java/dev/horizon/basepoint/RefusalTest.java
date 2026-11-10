package dev.horizon.basepoint;

import static dev.horizon.basepoint.expression.Expression.coalesce;
import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.concat;
import static dev.horizon.basepoint.expression.Expression.key;
import static dev.horizon.basepoint.expression.Expression.subquery;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.statement.Lock;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefusalTest {

  private final Dialect dialect = new StandardDialect();
  private final CollectionReference orders = dialect.collection("orders");
  private final CollectionReference items = dialect.collection("items");
  private final CollectionReference tree = dialect.collection("tree");

  private Arguments refusal(String name, ThrowingCallable call, Class<? extends Throwable> type, String message) {
    return argumentSet(name, call, type, message);
  }

  private Query start() {
    return items.query().columns("id").where(eq("id", 1));
  }

  private Query step() {
    return items.query().relation("parent", tree.query().on("id", "order_id")).columns("id");
  }

  Stream<Arguments> refusals() {
    return Stream.of(
        refusal("a limit of 0", () -> orders.query().limit(0), IllegalArgumentException.class,
            "The limit must be greater than 0, but is 0."),
        refusal("a negative offset", () -> orders.query().offset(-1), IllegalArgumentException.class,
            "The offset must be 0 or greater, but is -1."),
        refusal("a limit of 0 on a union", () -> orders.query().union(orders.query()).limit(0),
            IllegalArgumentException.class, "The limit must be greater than 0"),
        refusal("a lock without a mode", () -> orders.query().lock(null), NullPointerException.class, "mode"),
        refusal("a lock clause without a mode", () -> new Lock(null), NullPointerException.class, "mode"),
        refusal("an insert of no rows", () -> orders.insert(List.of()), IllegalArgumentException.class,
            "The insert has no rows."),
        refusal("rows of other columns", () -> orders.insert(List.of(Map.of("id", 1), Map.of("state", "OPEN"))),
            IllegalArgumentException.class, "Row 1 sets the columns [state], but the first row sets [id]."),
        refusal("two rows of default values", () -> orders.insert(List.of(Map.of(), Map.of())),
            IllegalArgumentException.class, "Only one row can use the default value of every column."),
        refusal("an upsert without a key", () -> orders.insert(Map.of("id", 1)).replace(),
            IllegalArgumentException.class, "The key has no columns."),
        refusal("an upsert of default values", () -> orders.insert(Map.of()).ignore("id"),
            IllegalArgumentException.class, "cannot use a key"),
        refusal("an update without assignments", () -> orders.update().where(eq("id", 1)).build(),
            IllegalStateException.class, "The update sets no columns."),
        refusal("an update without a filter", () -> orders.update().set("state", "OPEN").build(),
            UnfilteredWriteException.class, "The update has no filter."),
        refusal("a delete without a filter", () -> orders.delete().build(), UnfilteredWriteException.class,
            "The delete has no filter."),
        refusal("an empty key of a JSON object", () -> key("labels", ""), IllegalArgumentException.class,
            "A JSON key must not be empty"),
        refusal("a key of a JSON object with a quote", () -> key("labels", "a\"b"), IllegalArgumentException.class,
            "must not be empty or contain"),
        refusal("a key of a JSON object with a backslash", () -> key("labels", "a\\b"),
            IllegalArgumentException.class, "must not be empty or contain"),
        refusal("concat of one text", () -> concat(column("state")), IllegalArgumentException.class,
            "concat needs at least two texts, but has 1."),
        refusal("coalesce of one value", () -> coalesce(column("note")), IllegalArgumentException.class,
            "coalesce needs at least two values, but has 1."),
        refusal("on() in a query of its own", () -> items.query().on("order_id", "id").build(),
            IllegalStateException.class, "refers to an enclosing query, but there is none"),
        refusal("a subquery of two columns", () -> subquery(items.query().columns("sku", "qty")),
            InvalidQueryException.class, "selects 2 columns"),
        refusal("a subquery of all columns", () -> subquery(items.query()), InvalidQueryException.class,
            "selects all columns"),
        refusal("an include without a decorator of the dialect", () -> orders.query().include("items", items.query()
            .on("order_id", "id").columns("id")).build(), UnsupportedFeatureException.class,
            "does not render includes"),
        refusal("a sorted include of a column named bp_n", () -> orders.query().include("items", items.query()
            .on("order_id", "id").columns("bp_n").orderBy("id")), InvalidQueryException.class,
            "cannot select a column named bp_n"),
        refusal("a union of queries of two dialects", () -> orders.query().union(new StandardDialect()
            .collection("orders").query()), InvalidQueryException.class, "come from different dialects"),
        refusal("a WITH query in an include", () -> orders.query().include("items", items.query()
            .with("big", orders.query()).on("order_id", "id").columns("sku")), InvalidQueryException.class,
            "Call with() on the outermost query"),
        refusal("a WITH query in a subquery", () -> subquery(items.query().with("big", orders.query()).count()),
            InvalidQueryException.class, "Call with() on the outermost query"),
        refusal("a WITH query in exists()", () -> exists(items.query().with("big", orders.query())),
            InvalidQueryException.class, "Call with() on the outermost query"),
        refusal("a WITH query in an operand of a union", () -> orders.query().with("big", orders.query())
            .union(orders.query()), InvalidQueryException.class, "Call with() on the outermost query"),
        refusal("a WITH query with on()", () -> orders.query().with("big", orders.query().on("id", "order_id")),
            InvalidQueryException.class, "Leave out on()"),
        refusal("two WITH queries of one name", () -> orders.query().with("big", orders.query()).with("big",
            items.query()), InvalidQueryException.class, "The query has two WITH queries named big."),
        refusal("a recursive start that reads the WITH query", () -> tree.query().withRecursive("tree", step(),
            step()), InvalidQueryException.class, "The start of the recursive WITH query tree reads it."),
        refusal("a recursive step that does not read the WITH query", () -> tree.query().withRecursive("tree",
            start(), start()), InvalidQueryException.class, "does not read it"),
        refusal("a recursive part with on()", () -> tree.query().withRecursive("tree", start().on("id", "order_id"),
            step()), InvalidQueryException.class, "has on(), a limit or an offset"),
        refusal("a recursive part with a limit", () -> tree.query().withRecursive("tree", start(), step().limit(5)),
            InvalidQueryException.class, "has on(), a limit or an offset"),
        refusal("a relation without on()", () -> items.query().relation("order", orders.query()),
            InvalidQueryException.class, "have no on()"),
        refusal("a relation of selected rows", () -> items.query().relation("order", orders.query().on("id",
            "order_id").columns("id")), InvalidQueryException.class, "can have only on() and filters"),
        refusal("a relation of sorted rows", () -> items.query().relation("order", orders.query().on("id",
            "order_id").orderBy("id")), InvalidQueryException.class, "can have only on() and filters"),
        refusal("a relation of a union", () -> items.query().relation("order", orders.query().on("id", "order_id")
            .union(orders.query())), InvalidQueryException.class, "can have only on() and filters"),
        refusal("two relations of one name", () -> items.query().relation("order", orders.query().on("id",
            "order_id")).relation("order", orders.query().on("id", "order_id")), InvalidQueryException.class,
            "The query has two relations named order."),
        refusal("a relation that reads a later relation", () -> items.query().relation("archive", orders.query()
            .on("id", "order.id")), InvalidQueryException.class, "which the query does not name before it"),
        refusal("a path of a relation that the query does not name", () -> items.query().select("state",
            column("order", "state")).build(), IllegalStateException.class, "The query has no relation named order."),
        refusal("an outer column of a relation that the enclosing query does not name", () -> orders.query()
            .where(exists(items.query().on("order_id", "customer.id"))).build(), IllegalStateException.class,
            "reads the relation customer, which the enclosing query does not name"));
  }

  @ParameterizedTest
  @MethodSource("refusals")
  @DisplayName("Refuses")
  void refuse(ThrowingCallable call, Class<? extends Throwable> type, String message) {
    assertThatThrownBy(call).isInstanceOf(type).hasMessageContaining(message);
  }
}
