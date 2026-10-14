package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Combinable;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Query;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class CompoundQueryContractTest extends ContractTest {

  static Stream<Arguments> cases() {
    return Stream.of(
        arguments("union keeps duplicates", (Function<Sets, Combinable>) sets -> sets.open().union(sets.archived()),
            List.of(1, 1, 2, 5, 7)),
        arguments("union distinct",
            (Function<Sets, Combinable>) sets -> sets.open().union(sets.archived()).distinct(), List.of(1, 2, 5, 7)),
        arguments("intersect", (Function<Sets, Combinable>) sets -> sets.open().intersect(sets.archived()),
            List.of(1)),
        arguments("except", (Function<Sets, Combinable>) sets -> sets.open().except(sets.archived()), List.of(2, 5)),
        arguments("operators apply from left to right",
            (Function<Sets, Combinable>) sets -> sets.open().union(sets.archived()).except(sets.first()),
            List.of(2, 5, 7)),
        arguments("operand with its own limit",
            (Function<Sets, Combinable>) sets -> sets.firstTwo().union(sets.archived()), List.of(1, 1, 2, 7)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  void combinesRows(String name, Function<Sets, Combinable> combine, List<Integer> expected) throws SQLException {
    assertThat(sortedIds(combine.apply(sets()))).containsExactlyElementsOf(expected);
  }

  @Test
  void sortsAndLimitsTheCombinedRows() throws SQLException {
    Sets sets = sets();

    assertThat(idsInOrder(sets.open().union(sets.archived()).orderBy("id", Direction.DESC).limit(3)))
        .containsExactly(7, 5, 2);
  }

  @Test
  void skipsCombinedRows() throws SQLException {
    Sets sets = sets();

    assertThat(idsInOrder(sets.open().union(sets.archived()).distinct().orderBy("id").offset(2)))
        .containsExactly(5, 7);
  }

  private Sets sets() {
    return new Sets(orders.query().columns("id").where(eq("state", "OPEN")),
        archived.query().columns("id").where(eq("state", "OPEN")),
        orders.query().columns("id").where(eq("id", 1)),
        orders.query().columns("id").orderBy("id").limit(2));
  }

  record Sets(Query open, Query archived, Query first, Query firstTwo) {
  }
}
