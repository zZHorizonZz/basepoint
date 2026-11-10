package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.avg;
import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.count;
import static dev.horizon.basepoint.expression.Expression.max;
import static dev.horizon.basepoint.expression.Expression.min;
import static dev.horizon.basepoint.expression.Expression.sum;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.horizon.basepoint.Direction;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class AggregateContractTest extends ContractTest {

  @Test
  @DisplayName("Counts and sums the rows of each group, keeps the groups that the filter of groups keeps, and sorts"
      + " them by a sum")
  void group() throws SQLException {
    List<Map<String, Object>> groups = rows(orders.query().columns("state").select("orders", count())
        .select("revenue", sum("total")).groupBy("state").having(gt(sum("total"), 100))
        .orderBy("revenue", Direction.DESC));

    assertThat(groups).extracting(row -> row.get("state"), row -> number(row.get("orders")),
        row -> number(row.get("revenue"))).containsExactly(tuple("OPEN", 3L, 450L), tuple("CLOSED", 1L, 300L));
  }

  @Test
  @DisplayName("Gives the smallest value, the largest value and the number of values of all rows")
  void summarize() throws SQLException {
    Map<String, Object> row = rows(orders.query().select("lowest", min("total")).select("highest", max("total"))
        .select("noted", count("note"))).getFirst();

    assertThat(List.of(number(row.get("lowest")), number(row.get("highest")), number(row.get("noted"))))
        .containsExactly(50L, 300L, 2L);
  }

  @Test
  @DisplayName("Averages the values of a column")
  void average() throws SQLException {
    Object average = rows(orders.query().select("average", avg("total")).where(eq("state", "OPEN"))).getFirst()
        .get("average");

    assertThat(new BigDecimal(average.toString())).isEqualByComparingTo("150");
  }

  @Test
  @DisplayName("Counts the rows that a filter keeps")
  void countRows() throws SQLException {
    assertThat(number(rows(orders.query().where(eq("state", "OPEN")).count()).getFirst().get("count"))).isEqualTo(3);
  }

  @Test
  @DisplayName("Gives one row for the aggregates of no rows: 0 for a count, and NULL for the others")
  void aggregateNothing() throws SQLException {
    Map<String, Object> row = rows(orders.query().select("rows", count()).select("revenue", sum("total"))
        .select("highest", max("total")).where(eq("id", -1))).getFirst();

    assertThat(number(row.get("rows"))).isZero();
    assertThat(row.get("revenue")).isNull();
    assertThat(row.get("highest")).isNull();
  }

  @Test
  @DisplayName("Groups by two columns")
  void groupTwice() throws SQLException {
    List<Map<String, Object>> groups = rows(orders.query().columns("state", "region").count()
        .groupBy("state", "region").where(eq("state", "OPEN")).orderBy("region"));

    assertThat(groups).extracting(row -> row.get("region"), row -> number(row.get("count")))
        .containsExactly(tuple("EU", 1L), tuple("US", 2L));
  }

  @Test
  @DisplayName("Filters groups by a column of the group and by an aggregate together")
  void filterGroups() throws SQLException {
    List<Map<String, Object>> groups = rows(items.query().columns("order_id").select("quantity", sum("qty"))
        .groupBy(column("order_id")).having(gt(sum("qty"), 3)).having(gt(column("order_id"), 1))
        .orderBy("order_id"));

    assertThat(groups).extracting(row -> number(row.get("order_id"))).containsExactly(2L, 5L);
  }
}
