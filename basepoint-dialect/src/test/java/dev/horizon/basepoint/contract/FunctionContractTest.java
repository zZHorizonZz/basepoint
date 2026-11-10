package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.abs;
import static dev.horizon.basepoint.Expression.add;
import static dev.horizon.basepoint.Expression.coalesce;
import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Expression.concat;
import static dev.horizon.basepoint.Expression.divide;
import static dev.horizon.basepoint.Expression.length;
import static dev.horizon.basepoint.Expression.lower;
import static dev.horizon.basepoint.Expression.multiply;
import static dev.horizon.basepoint.Expression.now;
import static dev.horizon.basepoint.Expression.subtract;
import static dev.horizon.basepoint.Expression.trim;
import static dev.horizon.basepoint.Expression.upper;
import static dev.horizon.basepoint.Expression.value;
import static dev.horizon.basepoint.Expression.when;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.in;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Expression;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class FunctionContractTest extends ContractTest {

  private List<Object> values(String column, List<Map<String, Object>> rows) {
    return rows.stream().map(row -> row.get(column)).toList();
  }

  @Test
  void joinsTexts() throws SQLException {
    Expression label = concat(column("state"), value("-"), column("region"));

    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("label", label).where(in("id", List.of(1,
        4))).orderBy("id"));

    assertThat(values("label", rows)).containsExactly("OPEN-EU", null);
  }

  @Test
  void changesTexts() throws SQLException {
    List<Map<String, Object>> rows = rows(items.query().columns("id").select("lower", lower(column("sku")))
        .select("upper", upper(column("sku"))).select("trimmed", trim(value("  x "))).where(eq("id", 12)));

    assertThat(rows.getFirst()).containsEntry("lower", "a%b_c").containsEntry("upper", "A%B_C")
        .containsEntry("trimmed", "x");
  }

  @Test
  void countsCharactersWithTrailingSpaces() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("n", length(value("ab  ")))
        .select("state", length(column("state"))).where(eq("id", 4)));

    assertThat(number(rows.getFirst().get("n"))).isEqualTo(4);
    assertThat(number(rows.getFirst().get("state"))).isEqualTo(9);
  }

  @Test
  void calculates() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id")
        .select("sum", add(column("total"), value(5)))
        .select("difference", abs(subtract(column("total"), value(200))))
        .select("product", multiply(column("total"), value(2)))
        .select("quotient", divide(column("total"), value(10)))
        .where(eq("id", 1)));

    Map<String, Object> row = rows.getFirst();
    assertThat(List.of(number(row.get("sum")), number(row.get("difference")), number(row.get("product")),
        number(row.get("quotient")))).containsExactly(155L, 50L, 300L, 15L);
  }

  @Test
  void takesTheFirstValueThatIsNotNull() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("place", coalesce(column("note"),
        column("region"), value("none"))).orderBy("id"));

    assertThat(values("place", rows)).containsExactly("EU", "rush", "gift", "none", "US");
  }

  @Test
  void choosesByACondition() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id").select("size", when(gt("total", 200), value(
        "big"), value("small"))).orderBy("id"));

    assertThat(values("size", rows)).containsExactly("small", "small", "big", "small", "big");
  }

  @Test
  void filtersAndSortsByFunctions() throws SQLException {
    assertThat(idsInOrder(orders.query().columns("id").where(eq(lower(column("state")), "open"))
        .orderBy(length(column("region")), Direction.DESC).orderBy("id"))).containsExactly(1, 2, 5);
  }

  @Test
  void givesTheCurrentTime() throws SQLException {
    assertThat(rows(orders.query().columns("id").select("now", now()).where(eq("id", 1))).getFirst().get("now"))
        .isNotNull();
  }

  @Test
  void refusesFewerThanTwoValues() {
    assertThatThrownBy(() -> concat(column("state"))).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least two");
    assertThatThrownBy(() -> coalesce(column("note"))).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least two");
  }
}
