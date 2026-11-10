package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
import static dev.horizon.basepoint.filter.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class UnionContractTest extends ContractTest {

  private Query open() {
    return orders.query().columns("id").where(eq("state", "OPEN"));
  }

  private Query archivedOpen() {
    return archived.query().columns("id").where(eq("state", "OPEN"));
  }

  private Query first() {
    return orders.query().columns("id").where(eq("id", 1));
  }

  Stream<Arguments> combinations() {
    return Stream.of(
        argumentSet("union keeps duplicates", (Supplier<Query>) () -> open().union(archivedOpen()),
            List.of(1, 1, 2, 5, 7)),
        argumentSet("union with distinct gives each row once",
            (Supplier<Query>) () -> open().union(archivedOpen()).distinct(), List.of(1, 2, 5, 7)),
        argumentSet("intersect", (Supplier<Query>) () -> open().intersect(archivedOpen()), List.of(1)),
        argumentSet("except", (Supplier<Query>) () -> open().except(archivedOpen()), List.of(2, 5)),
        argumentSet("operators apply from left to right",
            (Supplier<Query>) () -> open().union(archivedOpen()).except(first()), List.of(2, 5, 7)),
        argumentSet("a union as the right operand",
            (Supplier<Query>) () -> first().except(archivedOpen().union(first())), List.of()),
        argumentSet("three unions", (Supplier<Query>) () -> first().union(first()).union(first()), List.of(1, 1, 1)),
        argumentSet("an operand with its own limit",
            (Supplier<Query>) () -> orders.query().columns("id").orderBy("id").limit(2).union(archivedOpen()),
            List.of(1, 1, 2, 7)),
        argumentSet("a filter of the combined rows",
            (Supplier<Query>) () -> open().union(archivedOpen()).where(gt("id", 1)), List.of(2, 5, 7)),
        argumentSet("a union of the same query", (Supplier<Query>) () -> open().union(open()).distinct(),
            List.of(1, 2, 5)));
  }

  @ParameterizedTest
  @MethodSource("combinations")
  @DisplayName("Combines the rows of queries")
  void combine(Supplier<Query> query, List<Integer> expected) throws SQLException {
    assertThat(sortedIds(query.get().build())).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("Sorts and limits the combined rows")
  void sortAndLimit() throws SQLException {
    assertThat(idsInOrder(open().union(archivedOpen()).orderBy("id", Direction.DESC).limit(3)))
        .containsExactly(7, 5, 2);
  }

  @Test
  @DisplayName("Skips combined rows")
  void skip() throws SQLException {
    assertThat(idsInOrder(open().union(archivedOpen()).distinct().orderBy("id").offset(2))).containsExactly(5, 7);
  }

  @Test
  @DisplayName("Relates both operands of a union in exists() to the row of the enclosing query")
  void relateOperands() throws SQLException {
    Query mugs = items.query().columns("id").on("order_id", "id").where(eq("sku", "MUG"));
    Query plates = items.query().columns("id").on("order_id", "id").where(eq("sku", "PLATE"));

    assertThat(ids(orders.query().columns("id").where(exists(mugs.union(plates))))).containsExactly(1, 2, 5);
  }

  @Test
  @DisplayName("Counts the combined rows")
  void countCombined() throws SQLException {
    assertThat(number(rows(open().union(archivedOpen()).count()).getFirst().get("count"))).isEqualTo(5);
  }
}
