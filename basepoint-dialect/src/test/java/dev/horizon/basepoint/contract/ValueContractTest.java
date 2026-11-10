package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.contains;
import static dev.horizon.basepoint.filter.Filter.endsWith;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static dev.horizon.basepoint.filter.Filter.lt;
import static dev.horizon.basepoint.filter.Filter.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.horizon.basepoint.CollectionReference;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class ValueContractTest extends ContractTest {

  private final List<String> texts = List.of("O'Brien", "say \"hi\"", "back\\slash", "'; DROP TABLE orders; --",
      "100%", "under_score", "[bracket]", "bang!", "a%b_c[d]!e", "Zürich", "日本語", "🦀 crab", "", "tab\there",
      "line\nbreak", "?", "$1", "@p1", "NULL", "x".repeat(1000));
  private CollectionReference table;

  @BeforeEach
  void insertTexts() throws SQLException {
    table = dialect.collection("texts");
    List<Map<String, Object>> rows = new ArrayList<>();
    for (int i = 0; i < texts.size(); i++) {
      rows.add(Map.of("id", i + 1, "body", texts.get(i)));
    }
    assertThat(execute(table.insert(rows))).isEqualTo(texts.size());
  }

  Stream<Arguments> textCases() {
    return IntStream.range(0, texts.size()).mapToObj(i -> argumentSet(describe(texts.get(i)), i + 1, texts.get(i)));
  }

  private String describe(String text) {
    String shown = text.length() > 20 ? text.substring(0, 20) + "... (" + text.length() + " characters)" : text;
    return text.isEmpty() ? "an empty text" : "[" + shown.replace("\n", "\\n").replace("\t", "\\t") + "]";
  }

  @ParameterizedTest
  @MethodSource("textCases")
  @DisplayName("Reads a text back as it was written")
  void readBack(int id, String text) throws SQLException {
    assertThat(valuesOf("body", rows(table.query().columns("body").where(eq("id", id))))).containsExactly(text);
  }

  @ParameterizedTest
  @MethodSource("textCases")
  @DisplayName("Finds a text by equality, and only that text")
  void findEqual(int id, String text) throws SQLException {
    assertThat(ids(table.query().columns("id").where(eq("body", text)))).containsExactly(id);
  }

  @ParameterizedTest
  @MethodSource("textCases")
  @DisplayName("Writes a text with an update")
  void update(int id, String text) throws SQLException {
    execute(table.update().set("body", text).where(eq("id", 1)));

    assertThat(valuesOf("body", rows(table.query().columns("body").where(eq("id", 1))))).containsExactly(text);
  }

  Stream<String> parts() {
    return Stream.of("%", "_", "[", "]", "!", "'", "\"", "\\", "?", "ürich", "本", "🦀", "\n");
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("parts")
  @DisplayName("Finds the texts that contain a part, which matches only itself")
  void findPart(String part) throws SQLException {
    assertThat(ids(table.query().columns("id").where(contains("body", part)))).containsExactlyElementsOf(
        IntStream.range(0, texts.size()).filter(i -> texts.get(i).contains(part)).mapToObj(i -> i + 1).toList());
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("parts")
  @DisplayName("Finds the texts that start and end with a part")
  void findEnds(String part) throws SQLException {
    assertThat(ids(table.query().columns("id").where(startsWith("body", part)))).containsExactlyElementsOf(
        IntStream.range(0, texts.size()).filter(i -> texts.get(i).startsWith(part)).mapToObj(i -> i + 1).toList());
    assertThat(ids(table.query().columns("id").where(endsWith("body", part)))).containsExactlyElementsOf(
        IntStream.range(0, texts.size()).filter(i -> texts.get(i).endsWith(part)).mapToObj(i -> i + 1).toList());
  }

  @Test
  @DisplayName("Leaves the table of a text that reads as SQL alone")
  void injection() throws SQLException {
    assertThat(ids(orders.query())).containsExactly(1, 2, 3, 4, 5);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("textCases")
  @DisplayName("Finds a text by filter text")
  void findByText(int id, String text) throws SQLException {
    assumeTrue(!text.contains("\"") && !text.contains("\\") && !text.contains("*") && !text.contains("\n"));

    assertThat(ids(table.query().columns("id").where("body = \"" + text + "\""))).containsExactly(id);
  }

  Stream<Arguments> numbers() {
    return Stream.of(
        argumentSet("the largest long", "whole", Long.MAX_VALUE),
        argumentSet("the smallest long", "whole", Long.MIN_VALUE),
        argumentSet("0", "whole", 0L),
        argumentSet("a large double", "approximate", 1.0e300),
        argumentSet("a small double", "approximate", -2.5e-300),
        argumentSet("a decimal with a fraction", "exact", new BigDecimal("-0.2500000000")));
  }

  @ParameterizedTest
  @MethodSource("numbers")
  @DisplayName("Reads a number back and finds it by equality")
  void number(String column, Object value) throws SQLException {
    CollectionReference numbers = dialect.collection("numbers");
    execute(numbers.insert(Map.of("id", 1, column, value)));

    Object read = rows(numbers.query().columns(column).where(eq("id", 1))).getFirst().get(column);
    assertThat(new BigDecimal(read.toString())).isEqualByComparingTo(new BigDecimal(value.toString()));
    assertThat(ids(numbers.query().columns("id").where(eq(column, value)))).containsExactly(1);
  }

  @Test
  @DisplayName("Keeps every digit of a decimal of 38 digits where the database stores decimals exactly")
  void exactDecimal() throws SQLException {
    assumeTrue(database.keepsDecimals());
    CollectionReference numbers = dialect.collection("numbers");
    BigDecimal value = new BigDecimal("1234567890123456789012345678.0123456789");
    execute(numbers.insert(Map.of("id", 1, "exact", value)));

    assertThat(new BigDecimal(rows(numbers.query().columns("exact")).getFirst().get("exact").toString()))
        .isEqualByComparingTo(value);
    assertThat(ids(numbers.query().columns("id").where(gt("exact", value.subtract(new BigDecimal("0.0000000001"))))
        .where(lt("exact", value.add(new BigDecimal("0.0000000001")))))).containsExactly(1);
  }
}
