package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.key;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.in;
import static dev.horizon.basepoint.Filter.ne;
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

public abstract class JsonContractTest extends ContractTest {

  static Stream<Arguments> filters() {
    return Stream.of(
        arguments("eq", eq(key("labels", "env"), "prod"), List.of(1, 3)),
        arguments("ne", ne(key("labels", "env"), "prod"), List.of(2)),
        arguments("has the key", ne(key("labels", "env"), null), List.of(1, 2, 3)),
        arguments("lacks the key", eq(key("labels", "env"), null), List.of(4, 5)),
        arguments("in", in(key("labels", "team"), List.of("core", "web")), List.of(1, 2)),
        arguments("a prefix of a key", eq(key("labels", "en"), "prod"), List.of()),
        arguments("a key with dots and a slash", eq(key("labels", "app.kubernetes.io/name"), "shop"), List.of(3)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("filters")
  void filtersByAKey(String name, Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(labelled.query().where(filter))).containsExactlyElementsOf(expected);
  }

  @Test
  void selectsTheValueOfAKey() throws SQLException {
    assertThat(rows(labelled.query().columns("id").select("env", key("labels", "env")).where(eq("id", 2))))
        .singleElement().extracting(row -> row.get("env")).isEqualTo("dev");
  }
}
