package dev.horizon.basepoint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.aquery.InvalidQueryException;
import dev.horizon.basepoint.contract.FilterCases;
import dev.horizon.basepoint.contract.FilterCases.ErrorCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterErrorTest {

  private final Dialect dialect = new StandardDialect();

  List<ErrorCase> cases() {
    return new FilterCases().errors();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("Refuses the filter text of the cases in filters/")
  void refuse(ErrorCase filter) {
    assertThatThrownBy(() -> dialect.collection(filter.table()).query().where(filter.syntax().parse(filter.text()))
        .build()).as(filter.text()).isInstanceOf(InvalidQueryException.class).hasMessageContaining(filter.message());
  }
}
