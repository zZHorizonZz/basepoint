package dev.horizon.basepoint.postgres;

import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.InvalidQueryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgresDialectTest {

  private final CollectionReference orders = new PostgresDialect().collection("orders");

  @Test
  @DisplayName("Writes the placeholders that its database clients expect by default")
  void placeholders() {
    assertThat(orders.query().where(eq("id", 1)).where(eq("state", "OPEN")).build().text()).contains("$1")
        .contains("$2");
  }

  @Test
  @DisplayName("Combines the queries of two dialects of the same placeholders, and refuses other placeholders")
  void combineEqual() {
    assertThat(orders.query().union(new PostgresDialect().collection("archived").query()).build().text())
        .contains("UNION ALL");
    assertThatThrownBy(() -> orders.query().union(new PostgresDialect(ParameterStyle.QUESTION_MARK).collection(
        "archived").query())).isInstanceOf(InvalidQueryException.class).hasMessageContaining("different dialects");
  }
}
