package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.abs;
import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.coalesce;
import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.concat;
import static dev.horizon.basepoint.expression.Expression.divide;
import static dev.horizon.basepoint.expression.Expression.length;
import static dev.horizon.basepoint.expression.Expression.lower;
import static dev.horizon.basepoint.expression.Expression.multiply;
import static dev.horizon.basepoint.expression.Expression.subtract;
import static dev.horizon.basepoint.expression.Expression.trim;
import static dev.horizon.basepoint.expression.Expression.upper;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.expression.Expression.when;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static dev.horizon.basepoint.filter.Filter.in;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.expression.Expression;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class FunctionContractTest extends ContractTest {

  @Test
  @DisplayName("Joins texts, and gives NULL when one of them is NULL")
  void join() throws SQLException {
    Expression label = concat(column("state"), value("-"), column("region"));

    assertThat(valuesOf("label", rows(orders.query().columns("id").select("label", label).where(in("id", List.of(1,
        4))).orderBy("id")))).containsExactly("OPEN-EU", null);
  }

  @Test
  @DisplayName("Writes a text in lower and upper case, and trims it")
  void changeCase() throws SQLException {
    Map<String, Object> row = rows(items.query().columns("id").select("lower", lower(column("sku")))
        .select("upper", upper(column("sku"))).select("trimmed", trim(value("  x "))).where(eq("id", 12))).getFirst();

    assertThat(row).containsEntry("lower", "a%b_c").containsEntry("upper", "A%B_C").containsEntry("trimmed", "x");
  }

  Stream<Arguments> lengths() {
    return Stream.of(argumentSet("a word", "OPEN", 4), argumentSet("spaces at the end count", "ab  ", 4),
        argumentSet("an empty text", "", 0), argumentSet("letters beyond ASCII count once", "Zürich", 6));
  }

  @ParameterizedTest
  @MethodSource("lengths")
  @DisplayName("Counts the characters of a text")
  void countCharacters(String text, int expected) throws SQLException {
    Object length = rows(orders.query().select("n", length(value(text))).where(eq("id", 1))).getFirst().get("n");

    assertThat(number(length)).isEqualTo(expected);
  }

  @Test
  @DisplayName("Adds, subtracts, multiplies and divides numbers")
  void calculate() throws SQLException {
    Map<String, Object> row = rows(orders.query().select("sum", add(column("total"), value(5)))
        .select("difference", abs(subtract(column("total"), value(200))))
        .select("product", multiply(column("total"), value(2)))
        .select("quotient", divide(column("total"), value(10))).where(eq("id", 1))).getFirst();

    assertThat(List.of(number(row.get("sum")), number(row.get("difference")), number(row.get("product")),
        number(row.get("quotient")))).containsExactly(155L, 50L, 300L, 15L);
  }

  @Test
  @DisplayName("Gives the first value that is not NULL")
  void takeFirst() throws SQLException {
    assertThat(valuesOf("place", rows(orders.query().columns("id").select("place", coalesce(column("note"),
        column("region"), value("none"))).orderBy("id")))).containsExactly("EU", "rush", "gift", "none", "US");
  }

  @Test
  @DisplayName("Gives one of two values by a condition")
  void choose() throws SQLException {
    assertThat(valuesOf("size", rows(orders.query().columns("id").select("size", when(gt("total", 200), value("big"),
        value("small"))).orderBy("id")))).containsExactly("small", "small", "big", "small", "big");
  }

  @Test
  @DisplayName("Filters and sorts by functions")
  void filterAndSort() throws SQLException {
    assertThat(idsInOrder(orders.query().columns("id").where(gt(length(column("state")), 3))
        .orderBy(length(column("state")), Direction.DESC).orderBy("id"))).containsExactly(4, 3, 1, 2, 5);
  }

  @Test
  @DisplayName("Nests functions in functions")
  void nest() throws SQLException {
    Expression label = upper(concat(trim(value(" a ")), lower(column("state"))));

    assertThat(valuesOf("label", rows(orders.query().select("label", label).where(eq("id", 3)))))
        .containsExactly("ACLOSED");
  }
}
