package dev.horizon.basepoint.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class PagingContractTest extends ContractTest {

  static Stream<Arguments> pageCases() {
    return Stream.of(
        arguments("limit", (UnaryOperator<Query>) query -> query.orderBy("id").limit(2), List.of(1, 2)),
        arguments("offset", (UnaryOperator<Query>) query -> query.orderBy("id").offset(3), List.of(4, 5)),
        arguments("limit and offset", (UnaryOperator<Query>) query -> query.orderBy("id").offset(1).limit(2),
            List.of(2, 3)),
        arguments("descending", (UnaryOperator<Query>) query -> query.orderBy("total", Direction.DESC).limit(2),
            List.of(3, 5)),
        arguments("two sort keys",
            (UnaryOperator<Query>) query -> query.orderBy("state").orderBy("total", Direction.DESC),
            List.of(4, 3, 5, 1, 2)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pageCases")
  void returnsPage(String name, UnaryOperator<Query> page, List<Integer> expected) throws SQLException {
    assertThat(idsInOrder(page.apply(orders.query()))).containsExactlyElementsOf(expected);
  }

  @Test
  void limitsWithoutOrder() throws SQLException {
    assertThat(idsInOrder(orders.query().limit(3))).hasSize(3);
  }

  @Test
  void offsetsWithoutOrder() throws SQLException {
    assertThat(idsInOrder(orders.query().offset(2))).hasSize(3);
  }

  @Test
  void removesDuplicates() throws SQLException {
    List<Object> states = rows(orders.query().columns("state").distinct().orderBy("state")).stream()
        .map(row -> row.get("state")).toList();

    assertThat(states).containsExactly("CANCELLED", "CLOSED", "OPEN");
  }
}
