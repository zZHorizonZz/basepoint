package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.count;
import static dev.horizon.basepoint.Expression.max;
import static dev.horizon.basepoint.Expression.min;
import static dev.horizon.basepoint.Expression.sum;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.horizon.basepoint.Direction;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class AggregateContractTest extends ContractTest {

  @Test
  void groupsAndFiltersGroups() throws SQLException {
    List<Map<String, Object>> groups = rows(orders.query()
        .columns("state")
        .select("orders", count())
        .select("revenue", sum("total"))
        .groupBy("state")
        .having(gt(sum("total"), 100))
        .orderBy("revenue", Direction.DESC));

    assertThat(groups)
        .extracting(row -> row.get("state"), row -> number(row.get("orders")), row -> number(row.get("revenue")))
        .containsExactly(tuple("OPEN", 3L, 450L), tuple("CLOSED", 1L, 300L));
  }

  @Test
  void aggregatesWithoutGroups() throws SQLException {
    Map<String, Object> row = rows(orders.query()
        .select("lowest", min("total"))
        .select("highest", max("total"))
        .select("noted", count("note"))).getFirst();

    assertThat(number(row.get("lowest"))).isEqualTo(50L);
    assertThat(number(row.get("highest"))).isEqualTo(300L);
    assertThat(number(row.get("noted"))).isEqualTo(2L);
  }

  @Test
  void countsRows() throws SQLException {
    Map<String, Object> row = rows(orders.query().where(eq("state", "OPEN")).count()).getFirst();

    assertThat(number(row.get("count"))).isEqualTo(3L);
  }
}
