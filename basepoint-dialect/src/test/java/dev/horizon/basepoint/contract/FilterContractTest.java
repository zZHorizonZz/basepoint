package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Filter.and;
import static dev.horizon.basepoint.Filter.between;
import static dev.horizon.basepoint.Filter.contains;
import static dev.horizon.basepoint.Filter.endsWith;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.gte;
import static dev.horizon.basepoint.Filter.in;
import static dev.horizon.basepoint.Filter.isNotNull;
import static dev.horizon.basepoint.Filter.isNull;
import static dev.horizon.basepoint.Filter.like;
import static dev.horizon.basepoint.Filter.lt;
import static dev.horizon.basepoint.Filter.lte;
import static dev.horizon.basepoint.Filter.ne;
import static dev.horizon.basepoint.Filter.nin;
import static dev.horizon.basepoint.Filter.not;
import static dev.horizon.basepoint.Filter.or;
import static dev.horizon.basepoint.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Filter;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class FilterContractTest extends ContractTest {

  static Stream<Arguments> orderFilters() {
    return Stream.of(
        arguments("eq", eq("state", "OPEN"), List.of(1, 2, 5)),
        arguments("eq null", eq("note", null), List.of(1, 4, 5)),
        arguments("ne", ne("state", "OPEN"), List.of(3, 4)),
        arguments("ne null", ne("note", null), List.of(2, 3)),
        arguments("gt", gt("total", 150), List.of(3, 5)),
        arguments("gte", gte("total", 150), List.of(1, 3, 5)),
        arguments("lt", lt("total", 80), List.of(4)),
        arguments("lte", lte("total", 80), List.of(2, 4)),
        arguments("between", between("total", 80, 150), List.of(1, 2)),
        arguments("in", in("region", List.of("EU", "US")), List.of(1, 2, 3, 5)),
        arguments("in empty", in("region", List.of()), List.of()),
        arguments("nin", nin("region", List.of("EU")), List.of(2, 5)),
        arguments("nin empty", nin("id", List.of()), List.of(1, 2, 3, 4, 5)),
        arguments("isNull", isNull("region"), List.of(4)),
        arguments("isNotNull", isNotNull("note"), List.of(2, 3)),
        arguments("and", and(eq("state", "OPEN"), gt("total", 100)), List.of(1, 5)),
        arguments("and empty", and(), List.of(1, 2, 3, 4, 5)),
        arguments("or", or(eq("region", "US"), gte("total", 300)), List.of(2, 3, 5)),
        arguments("or empty", or(), List.of()),
        arguments("not", not(eq("state", "OPEN")), List.of(3, 4)),
        arguments("column to column", lt("id", column("total")), List.of(1, 2, 3, 4, 5)));
  }

  static Stream<Arguments> itemFilters() {
    return Stream.of(
        arguments("contains escapes % and _", contains("sku", "%b_"), List.of(12)),
        arguments("startsWith escapes %", startsWith("sku", "a%"), List.of(12)),
        arguments("endsWith escapes _", endsWith("sku", "_c"), List.of(12)),
        arguments("contains escapes [", contains("sku", "[1]"), List.of(14)),
        arguments("like keeps wildcards", like("sku", "M%"), List.of(10, 13)),
        arguments("column to column", gt("qty", column("order_id")), List.of(10, 12, 13)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("orderFilters")
  void filtersOrders(String name, Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(orders.query().where(filter))).containsExactlyElementsOf(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("itemFilters")
  void filtersItems(String name, Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(items.query().where(filter))).containsExactlyElementsOf(expected);
  }

  @Test
  void combinesFiltersOfSeveralCalls() throws SQLException {
    assertThat(ids(orders.query().where(ne("state", "OPEN")).where(lt("total", 100)))).containsExactly(4);
  }
}
