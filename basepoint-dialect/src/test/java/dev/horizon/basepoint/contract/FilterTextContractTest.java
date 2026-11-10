package dev.horizon.basepoint.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.contract.FilterCases.RowCase;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class FilterTextContractTest extends ContractTest {

  List<RowCase> cases() {
    return new FilterCases().rows();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("Keeps the rows of the cases in filters/")
  void filter(RowCase filter) throws SQLException {
    List<String> rows = valuesOf(filter.key(), rows(dialect.collection(filter.table()).query().columns(filter.key())
        .where(filter.syntax().parse(filter.text())).orderBy(filter.key()))).stream().map(String::valueOf).toList();

    assertThat(rows).as(filter.text()).containsExactlyElementsOf(filter.rows());
  }
}
