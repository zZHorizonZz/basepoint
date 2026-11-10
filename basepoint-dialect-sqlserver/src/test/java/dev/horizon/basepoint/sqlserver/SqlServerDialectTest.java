package dev.horizon.basepoint.sqlserver;

import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.now;
import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.UnsupportedFeatureException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlServerDialectTest {

  private final CollectionReference orders = new SqlServerDialect().collection("orders");

  @Test
  @DisplayName("Writes the placeholders that its database clients expect by default")
  void placeholders() {
    assertThat(orders.query().where(eq("id", 1)).where(eq("state", "OPEN")).build().text()).contains("@p1")
        .contains("@p2");
  }

  @Test
  @DisplayName("Refuses to move a timestamp by more than SQL Server can")
  void refuseFarShift() {
    assertThatThrownBy(() -> orders.query().select("later", add(now(), Duration.ofDays(365L * 100))).build())
        .isInstanceOf(UnsupportedFeatureException.class).hasMessageContaining("at most 68 years");
  }
}
