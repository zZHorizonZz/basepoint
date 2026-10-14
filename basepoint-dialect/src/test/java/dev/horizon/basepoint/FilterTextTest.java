package dev.horizon.basepoint;

import static dev.horizon.basepoint.Filter.and;
import static dev.horizon.basepoint.Filter.endsWith;
import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.gte;
import static dev.horizon.basepoint.Filter.isNotNull;
import static dev.horizon.basepoint.Filter.isNull;
import static dev.horizon.basepoint.Filter.lt;
import static dev.horizon.basepoint.Filter.lte;
import static dev.horizon.basepoint.Filter.ne;
import static dev.horizon.basepoint.Filter.not;
import static dev.horizon.basepoint.Filter.or;
import static dev.horizon.basepoint.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.aquery.InvalidQueryException;
import dev.horizon.aquery.cel.CelFilterParser;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class FilterTextTest {

  static Stream<Arguments> aip160() {
    return Stream.of(
        arguments("state = OPEN", eq("state", "OPEN")),
        arguments("state = \"OPEN\"", eq("state", "OPEN")),
        arguments("state != OPEN", ne("state", "OPEN")),
        arguments("total > 100", gt("total", 100L)),
        arguments("total >= 1.5", gte("total", new BigDecimal("1.5"))),
        arguments("total < 2e3", lt("total", new BigDecimal("2e3"))),
        arguments("total <= -5", lte("total", -5L)),
        arguments("total = 99999999999999999999", eq("total", new BigDecimal("99999999999999999999"))),
        arguments("active = true", eq("active", true)),
        arguments("active = false", eq("active", false)),
        arguments("note = null", isNull("note")),
        arguments("note != null", isNotNull("note")),
        arguments("note:*", isNotNull("note")),
        arguments("state = \"C*\"", startsWith("state", "C")),
        arguments("state = \"*N\"", endsWith("state", "N")),
        arguments("state = \"A*Z\"", new Filter.Match(Expression.column("state"), List.of("A", "Z"), false)),
        arguments("state != \"C*\"", new Filter.Match(Expression.column("state"), List.of("C", ""), true)),
        arguments("a = 1 AND b = 2", and(eq("a", 1L), eq("b", 2L))),
        arguments("a = 1 OR b = 2", or(eq("a", 1L), eq("b", 2L))),
        arguments("NOT a = 1", not(eq("a", 1L))),
        arguments("-a = 1", not(eq("a", 1L))),
        arguments("(a = 1 OR b = 2) AND c = 3", and(or(eq("a", 1L), eq("b", 2L)), eq("c", 3L))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("aip160")
  void parsesAip160(String text, Filter expected) {
    assertThat(Filter.of(text)).isEqualTo(expected);
  }

  static Stream<Arguments> cel() {
    return Stream.of(
        arguments("state == 'OPEN'", eq("state", "OPEN")),
        arguments("total > 100", gt("total", 100L)),
        arguments("state == 'OPEN' && total > 100", and(eq("state", "OPEN"), gt("total", 100L))),
        arguments("state == 'OPEN' || total > 100", or(eq("state", "OPEN"), gt("total", 100L))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cel")
  void parsesCel(String text, Filter expected) {
    assertThat(Filter.of(text, new CelFilterParser())).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = ';', quoteCharacter = '`', textBlock = """
      plain        ; The filter term 'plain' has no column.
      a.b = 1      ; Unsupported field path 'a.b'. Use a single column name.
      tags:urgent  ; The has operator supports only 'tags:*'
      total > null ; Cannot compare null with >. Use = or != with null.
      """)
  void rejectsUnsupportedFilters(String text, String message) {
    assertThatThrownBy(() -> Filter.of(text)).isInstanceOf(InvalidQueryException.class).hasMessageContaining(message);
  }
}
