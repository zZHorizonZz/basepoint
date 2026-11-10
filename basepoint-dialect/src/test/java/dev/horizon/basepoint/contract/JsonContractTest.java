package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.key;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.in;
import static dev.horizon.basepoint.filter.Filter.ne;
import static dev.horizon.basepoint.filter.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.filter.Filter;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class JsonContractTest extends ContractTest {

  Stream<Arguments> filters() {
    return Stream.of(
        argumentSet("eq", eq(key("labels", "env"), "prod"), List.of(1, 3)),
        argumentSet("ne leaves out the objects without the key", ne(key("labels", "env"), "prod"), List.of(2)),
        argumentSet("ne of null keeps the objects with the key", ne(key("labels", "env"), null), List.of(1, 2, 3)),
        argumentSet("eq of null keeps the objects without the key, and NULL", eq(key("labels", "env"), null),
            List.of(4, 5, 6)),
        argumentSet("a number compares as text", eq(key("labels", "replicas"), "3"), List.of(6)),
        argumentSet("in", in(key("labels", "team"), List.of("core", "web")), List.of(1, 2)),
        argumentSet("a prefix of a key is no key", eq(key("labels", "en"), "prod"), List.of()),
        argumentSet("a key with dots and a slash", eq(key("labels", "app.kubernetes.io/name"), "shop"), List.of(3)),
        argumentSet("startsWith", startsWith(key("labels", "team"), "c"), List.of(1)));
  }

  @ParameterizedTest
  @MethodSource("filters")
  @DisplayName("Filters by a key of a JSON object")
  void filter(Filter filter, List<Integer> expected) throws SQLException {
    assertThat(ids(labelled.query().where(filter))).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("Selects the value of a key, and NULL without the key")
  void select() throws SQLException {
    assertThat(valuesOf("env", rows(labelled.query().columns("id").select("env", key("labels", "env"))
        .orderBy("id")))).containsExactly("prod", "dev", "prod", null, null, null);
  }

  @Test
  @DisplayName("Sorts by the value of a key")
  void sort() throws SQLException {
    assertThat(idsInOrder(labelled.query().columns("id").where(ne(key("labels", "team"), null))
        .orderBy(key("labels", "team"), Direction.DESC))).containsExactly(2, 1);
  }
}
