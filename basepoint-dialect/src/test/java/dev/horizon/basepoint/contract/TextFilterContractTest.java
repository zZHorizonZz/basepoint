package dev.horizon.basepoint.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.aquery.cel.CelFilterParser;
import dev.horizon.basepoint.Filter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public abstract class TextFilterContractTest extends ContractTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = ';', quoteCharacter = '`', textBlock = """
      state = "OPEN" AND total > 100               ; 1 5
      region = EU                                  ; 1 3
      total >= 220 OR region = US                  ; 2 3 5
      NOT state = OPEN                             ; 3 4
      -state = OPEN                                ; 3 4
      note:*                                       ; 2 3
      note = null                                  ; 1 4 5
      note != null                                 ; 2 3
      state = "C*"                                 ; 3 4
      state = "*N"                                 ; 1 2 5
      (region = EU OR region = US) AND total < 200 ; 1 2
      """)
  void filtersWithAip160(String text, String expected) throws SQLException {
    assertThat(ids(orders.query().where(text))).containsExactlyElementsOf(idList(expected));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = ';', quoteCharacter = '`', textBlock = """
      state == 'OPEN' && total > 100 ; 1 5
      region == 'EU' || total < 60   ; 1 3 4
      !(state == 'OPEN')             ; 3 4
      """)
  void filtersWithCel(String text, String expected) throws SQLException {
    Filter filter = Filter.of(text, new CelFilterParser());

    assertThat(ids(orders.query().where(filter))).containsExactlyElementsOf(idList(expected));
  }

  private static List<Integer> idList(String expected) {
    return Arrays.stream(expected.trim().split(" ")).map(Integer::valueOf).toList();
  }
}
