package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.filter.Filter.and;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
import static dev.horizon.basepoint.filter.Filter.in;
import static dev.horizon.basepoint.filter.Filter.ne;
import static dev.horizon.basepoint.filter.Filter.nin;
import static dev.horizon.basepoint.filter.Filter.not;
import static dev.horizon.basepoint.filter.Filter.or;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.filter.Filter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public abstract class NestingContractTest extends ContractTest {

  @ParameterizedTest(name = "{0} levels")
  @ValueSource(ints = { 1, 10, 40 })
  @DisplayName("Keeps the rows of a filter nested in AND, OR and NOT")
  void nestFilters(int levels) throws SQLException {
    Filter filter = eq("state", "OPEN");
    for (int level = 0; level < levels; level++) {
      filter = and(or(filter, eq("id", -level)), not(eq("id", -level - 1000)));
    }

    assertThat(ids(orders.query().columns("id").where(filter))).containsExactly(1, 2, 5);
  }

  @ParameterizedTest(name = "{0} levels")
  @ValueSource(ints = { 1, 30, 64 })
  @DisplayName("Keeps the rows of filter text nested in parentheses, up to the 64 levels of the parser")
  void nestText(int levels) throws SQLException {
    String text = "(".repeat(levels) + "state = OPEN" + ")".repeat(levels);

    assertThat(ids(orders.query().columns("id").where(text))).containsExactly(1, 2, 5);
  }

  @ParameterizedTest(name = "{0} terms")
  @ValueSource(ints = { 2, 100, 500 })
  @DisplayName("Keeps the rows of filter text with many terms")
  void longText(int terms) throws SQLException {
    String text = IntStream.rangeClosed(1, terms).mapToObj(id -> "id = " + id).collect(Collectors.joining(" OR "));

    assertThat(ids(orders.query().columns("id").where(text))).containsExactlyElementsOf(List.of(1, 2, 3, 4, 5)
        .subList(0, Math.min(terms, 5)));
  }

  @ParameterizedTest(name = "{0} values")
  @ValueSource(ints = { 1, 100, 2000 })
  @DisplayName("Keeps the rows of a list of many values")
  void longList(int values) throws SQLException {
    List<Integer> ids = IntStream.rangeClosed(1, values).boxed().toList();

    assertThat(ids(orders.query().columns("id").where(in("id", ids)))).containsExactlyElementsOf(ids.subList(0, Math
        .min(values, 5)));
    assertThat(ids(orders.query().columns("id").where(nin("id", ids)))).containsExactlyElementsOf(List.of(1, 2, 3, 4,
        5).subList(Math.min(values, 5), 5));
  }

  @Test
  @DisplayName("Keeps the rows of 500 filters together")
  void manyFilters() throws SQLException {
    Query query = orders.query().columns("id");
    for (int i = 1; i <= 500; i++) {
      query = query.where(ne("id", -i));
    }

    assertThat(ids(query)).containsExactly(1, 2, 3, 4, 5);
  }

  @ParameterizedTest(name = "{0} levels")
  @ValueSource(ints = { 1, 3, 6 })
  @DisplayName("Keeps the rows of exists() in exists()")
  void nestExists(int levels) throws SQLException {
    Query inner = products.query().on("sku", "sku");
    for (int level = 1; level < levels; level++) {
      inner = products.query().on("sku", "sku").where(exists(items.query().on("sku", "sku").where(exists(inner))));
    }
    Query lines = items.query().on("order_id", "id").where(exists(inner));

    assertThat(ids(orders.query().columns("id").where(exists(lines)))).containsExactly(1, 2, 5);
  }

  @Test
  @DisplayName("Includes rows in rows in rows")
  void nestIncludes() throws SQLException {
    Query sameSku = items.query().on("sku", "sku").columns("id").orderBy("id");
    Query product = products.query().on("sku", "sku").columns("name").include("lines", sameSku);
    Query lines = items.query().on("order_id", "id").columns("sku").includeOne("product", product).orderBy("id");

    assertThat(json(rows(orders.query().columns("id").include("items", lines).where(eq("id", 2))).getFirst()
        .get("items"))).isEqualTo(json("""
            [{"sku": "MUG", "product": {"name": "Mug", "lines": [{"id": 10}, {"id": 13}]}}]"""));
  }

  @ParameterizedTest(name = "{0} queries")
  @ValueSource(ints = { 2, 20 })
  @DisplayName("Combines many queries")
  void manyUnions(int queries) throws SQLException {
    Query first = orders.query().columns("id").where(eq("id", 1));
    Query union = first;
    for (int i = 1; i < queries; i++) {
      union = union.union(first);
    }

    assertThat(sortedIds(union.build())).hasSize(queries).containsOnly(1);
  }

  @ParameterizedTest(name = "{0} levels")
  @ValueSource(ints = { 1, 30 })
  @DisplayName("Calculates a sum nested in sums")
  void nestArithmetic(int levels) throws SQLException {
    Expression sum = value(0);
    for (int level = 0; level < levels; level++) {
      sum = add(sum, value(1));
    }

    assertThat(number(rows(orders.query().select("sum", sum).where(eq("id", 1))).getFirst().get("sum")))
        .isEqualTo(levels);
  }

  @Test
  @DisplayName("Selects 200 values")
  void manyColumns() throws SQLException {
    Query query = orders.query().columns("id");
    for (int i = 0; i < 200; i++) {
      query = query.select("c" + i, value(i));
    }

    Map<String, Object> row = rows(query.where(eq("id", 1))).getFirst();
    assertThat(row).hasSize(201);
    assertThat(number(row.get("c199"))).isEqualTo(199);
  }
}
