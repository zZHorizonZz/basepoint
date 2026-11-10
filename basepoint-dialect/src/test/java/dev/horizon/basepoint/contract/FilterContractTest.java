package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.lower;
import static dev.horizon.basepoint.expression.Expression.multiply;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.filter.Filter.and;
import static dev.horizon.basepoint.filter.Filter.between;
import static dev.horizon.basepoint.filter.Filter.contains;
import static dev.horizon.basepoint.filter.Filter.endsWith;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static dev.horizon.basepoint.filter.Filter.gte;
import static dev.horizon.basepoint.filter.Filter.in;
import static dev.horizon.basepoint.filter.Filter.isNotNull;
import static dev.horizon.basepoint.filter.Filter.isNull;
import static dev.horizon.basepoint.filter.Filter.like;
import static dev.horizon.basepoint.filter.Filter.lt;
import static dev.horizon.basepoint.filter.Filter.lte;
import static dev.horizon.basepoint.filter.Filter.ne;
import static dev.horizon.basepoint.filter.Filter.nin;
import static dev.horizon.basepoint.filter.Filter.not;
import static dev.horizon.basepoint.filter.Filter.or;
import static dev.horizon.basepoint.filter.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.filter.Filter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class FilterContractTest extends ContractTest {

  Stream<Arguments> orderFilters() {
    return Stream.of(
        argumentSet("eq", eq("state", "OPEN"), List.of(1, 2, 5)),
        argumentSet("eq of null is IS NULL", eq("note", null), List.of(1, 4, 5)),
        argumentSet("ne leaves out NULL", ne("region", "EU"), List.of(2, 5)),
        argumentSet("ne of null is IS NOT NULL", ne("note", null), List.of(2, 3)),
        argumentSet("gt", gt("total", 150), List.of(3, 5)),
        argumentSet("gte", gte("total", 150), List.of(1, 3, 5)),
        argumentSet("lt", lt("total", 80), List.of(4)),
        argumentSet("lte", lte("total", 80), List.of(2, 4)),
        argumentSet("between includes both ends", between("total", 80, 150), List.of(1, 2)),
        argumentSet("in", in("region", List.of("EU", "US")), List.of(1, 2, 3, 5)),
        argumentSet("in of nothing keeps no row", in("region", List.of()), List.of()),
        argumentSet("in of a null keeps NULL", in("note", Arrays.asList("rush", null)), List.of(1, 2, 4, 5)),
        argumentSet("in of only null", in("note", Arrays.asList((Object) null)), List.of(1, 4, 5)),
        argumentSet("nin", nin("region", List.of("EU")), List.of(2, 5)),
        argumentSet("nin of nothing keeps every row", nin("id", List.of()), List.of(1, 2, 3, 4, 5)),
        argumentSet("nin of a null leaves out NULL", nin("note", Arrays.asList("rush", null)), List.of(3)),
        argumentSet("nin of only null", nin("note", Arrays.asList((Object) null)), List.of(2, 3)),
        argumentSet("isNull", isNull("region"), List.of(4)),
        argumentSet("isNotNull", isNotNull("note"), List.of(2, 3)),
        argumentSet("eq of a boolean", eq("paid", true), List.of(1, 3)),
        argumentSet("and", and(eq("state", "OPEN"), gt("total", 100)), List.of(1, 5)),
        argumentSet("and of nothing keeps every row", and(), List.of(1, 2, 3, 4, 5)),
        argumentSet("or", or(eq("region", "US"), gte("total", 300)), List.of(2, 3, 5)),
        argumentSet("or of nothing keeps no row", or(), List.of()),
        argumentSet("not", not(eq("state", "OPEN")), List.of(3, 4)),
        argumentSet("a column compared with a value of other columns",
            gt(column("total"), multiply(column("id"), value(50))), List.of(1, 3)),
        argumentSet("isNull of a value", isNull(lower(column("region"))), List.of(4)),
        argumentSet("startsWith of a value", startsWith(lower(column("state")), "op"), List.of(1, 2, 5)));
  }

  @ParameterizedTest
  @MethodSource("orderFilters")
  @DisplayName("Filters orders")
  void filterOrders(Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(orders.query().where(filter))).containsExactlyElementsOf(expected);
  }

  Stream<Arguments> itemFilters() {
    return Stream.of(
        argumentSet("contains matches % and _ as themselves", contains("sku", "%b_"), List.of(12)),
        argumentSet("startsWith matches % as itself", startsWith("sku", "a%"), List.of(12)),
        argumentSet("endsWith matches _ as itself", endsWith("sku", "_c"), List.of(12)),
        argumentSet("contains matches [ as itself", contains("sku", "[1]"), List.of(14)),
        argumentSet("contains of nothing keeps every row", contains("sku", ""), List.of(10, 11, 12, 13, 14, 15)),
        argumentSet("like keeps the wildcards", like("sku", "M%"), List.of(10, 13)),
        argumentSet("like with _", like("sku", "_UP"), List.of(11)),
        argumentSet("a column compared with a column", gt("qty", column("order_id")), List.of(10, 12, 13)));
  }

  @ParameterizedTest
  @MethodSource("itemFilters")
  @DisplayName("Filters items")
  void filterItems(Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(items.query().where(filter))).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("Keeps the rows that match the filters of every call")
  void combine() throws SQLException {
    assertThat(ids(orders.query().where(ne("state", "OPEN")).where(lt("total", 100)))).containsExactly(4);
  }

  @Test
  @DisplayName("Combines a filter and filter text")
  void combineText() throws SQLException {
    assertThat(ids(orders.query().where(eq("region", "US")).where("total > 100"))).containsExactly(5);
  }
}
