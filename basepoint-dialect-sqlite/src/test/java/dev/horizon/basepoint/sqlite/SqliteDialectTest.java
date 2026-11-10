package dev.horizon.basepoint.sqlite;

import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.CollectionReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqliteDialectTest {

  private final CollectionReference orders = new SqliteDialect().collection("orders");

  @Test
  @DisplayName("Writes the placeholders that its database clients expect by default")
  void placeholders() {
    assertThat(orders.query().where(eq("id", 1)).where(eq("state", "OPEN")).build().text()).contains("?")
        .contains("?");
  }
}
