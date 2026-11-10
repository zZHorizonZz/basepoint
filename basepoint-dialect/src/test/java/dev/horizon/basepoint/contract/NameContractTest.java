package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Direction;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class NameContractTest extends ContractTest {

  private final Map<String, String> prefixes = Map.of("select", "s", "with space", "w", "MixedCase", "m", "a\"b", "q",
      "a`b", "b", "a]b", "r", "it's", "i", "ünï", "u");
  private CollectionReference odd;

  @BeforeEach
  void referTable() {
    odd = dialect.collection("odd \"names\"");
  }

  Stream<String> names() {
    return Stream.of("select", "with space", "MixedCase", "a\"b", "a`b", "a]b", "it's", "ünï");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("names")
  @DisplayName("Selects a column by its name")
  void select(String name) throws SQLException {
    assertThat(valuesOf(name.toLowerCase(), rows(odd.query().columns("id", name).orderBy("id"))))
        .containsExactly(prefixes.get(name) + "1", prefixes.get(name) + "2");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("names")
  @DisplayName("Filters by a column by its name")
  void filter(String name) throws SQLException {
    assertThat(ids(odd.query().columns("id").where(eq(name, prefixes.get(name) + "2")))).containsExactly(2);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("names")
  @DisplayName("Sorts by a column by its name")
  void sort(String name) throws SQLException {
    assertThat(idsInOrder(odd.query().columns("id").orderBy(name, Direction.DESC))).containsExactly(2, 1);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("names")
  @DisplayName("Updates a column by its name")
  void update(String name) throws SQLException {
    assertThat(execute(odd.update().set(name, "new").where(eq("id", 1)))).isEqualTo(1);
    assertThat(ids(odd.query().columns("id").where(eq(name, "new")))).containsExactly(1);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("names")
  @DisplayName("Selects a value under the name")
  void label(String name) throws SQLException {
    assertThat(number(rows(odd.query().select(name, value(7)).where(eq("id", 1))).getFirst().get(name
        .toLowerCase()))).isEqualTo(7);
  }

  @Test
  @DisplayName("Inserts a row into every column by its name")
  void insert() throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 3);
    names().forEach(name -> row.put(name, prefixes.get(name) + "3"));

    assertThat(execute(odd.insert(row))).isEqualTo(1);
    assertThat(rows(odd.query().where(eq("id", 3))).getFirst()).containsEntry("a\"b", "q3").containsEntry("it's",
        "i3");
  }

  @Test
  @DisplayName("Includes the columns as the keys of a JSON object")
  void include() throws SQLException {
    JsonNode lines = json(rows(orders.query().columns("id").include("odd", odd.query().on("id", "id")
        .columns(names().toArray(String[]::new)).orderBy("id")).where(eq("id", 2))).getFirst().get("odd"));

    assertThat(lines).hasSize(1);
    names().forEach(name -> assertThat(lines.get(0).get(name).asText()).as(name).isEqualTo(prefixes.get(name) + "2"));
  }

  @Test
  @DisplayName("Reads the table by its name in a relation and a subquery")
  void relate() throws SQLException {
    List<Map<String, Object>> rows = rows(orders.query().columns("id").relation("odd", odd.query().on("id", "id"))
        .select("reserved", column("odd", "select"))
        .where(eq("id", 1)));

    assertThat(valuesOf("reserved", rows)).containsExactly("s1");
  }
}
