package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.count;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class PagingContractTest extends ContractTest {

  Stream<Arguments> pages() {
    return Stream.of(
        argumentSet("a limit", (UnaryOperator<Query>) query -> query.orderBy("id").limit(2), List.of(1, 2)),
        argumentSet("an offset", (UnaryOperator<Query>) query -> query.orderBy("id").offset(3), List.of(4, 5)),
        argumentSet("an offset of 0", (UnaryOperator<Query>) query -> query.orderBy("id").offset(0),
            List.of(1, 2, 3, 4, 5)),
        argumentSet("a limit and an offset", (UnaryOperator<Query>) query -> query.orderBy("id").offset(1).limit(2),
            List.of(2, 3)),
        argumentSet("an offset past the last row", (UnaryOperator<Query>) query -> query.orderBy("id").offset(10),
            List.of()),
        argumentSet("a limit beyond the rows", (UnaryOperator<Query>) query -> query.orderBy("id").limit(100),
            List.of(1, 2, 3, 4, 5)),
        argumentSet("a later limit replaces the earlier one",
            (UnaryOperator<Query>) query -> query.orderBy("id").limit(1).limit(3), List.of(1, 2, 3)),
        argumentSet("descending", (UnaryOperator<Query>) query -> query.orderBy("total", Direction.DESC).limit(2),
            List.of(3, 5)),
        argumentSet("two sort keys",
            (UnaryOperator<Query>) query -> query.orderBy("state").orderBy("total", Direction.DESC),
            List.of(4, 3, 5, 1, 2)),
        argumentSet("a sort by a value",
            (UnaryOperator<Query>) query -> query.orderBy(column("total"), Direction.ASC).limit(2), List.of(4, 2)));
  }

  @ParameterizedTest
  @MethodSource("pages")
  @DisplayName("Gives a page of sorted rows")
  void page(UnaryOperator<Query> page, List<Integer> expected) throws SQLException {
    assertThat(idsInOrder(page.apply(orders.query()))).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("Limits the rows without a sort order")
  void limitUnsorted() throws SQLException {
    assertThat(idsInOrder(orders.query().limit(3))).hasSize(3).doesNotHaveDuplicates().isSubsetOf(1, 2, 3, 4, 5);
  }

  @Test
  @DisplayName("Skips rows without a sort order")
  void offsetUnsorted() throws SQLException {
    assertThat(idsInOrder(orders.query().offset(2))).hasSize(3).doesNotHaveDuplicates().isSubsetOf(1, 2, 3, 4, 5);
  }

  @Test
  @DisplayName("Gives each row once with distinct")
  void distinct() throws SQLException {
    assertThat(valuesOf("state", rows(orders.query().columns("state").distinct().orderBy("state"))))
        .containsExactly("CANCELLED", "CLOSED", "OPEN");
  }

  @Test
  @DisplayName("Sorts by a selected value by its name")
  void sortBySelected() throws SQLException {
    assertThat(valuesOf("state", rows(orders.query().columns("state").select("orders", count()).groupBy("state")
        .orderBy("orders", Direction.DESC).orderBy("state")))).containsExactly("OPEN", "CANCELLED", "CLOSED");
  }

  @Test
  @DisplayName("Sorts NULL as the database does, and keeps the rows")
  void sortNull() throws SQLException {
    assertThat(idsInOrder(orders.query().orderBy("region").orderBy("id"))).hasSize(5).containsSubsequence(1, 3)
        .containsSubsequence(2, 5);
  }
}
