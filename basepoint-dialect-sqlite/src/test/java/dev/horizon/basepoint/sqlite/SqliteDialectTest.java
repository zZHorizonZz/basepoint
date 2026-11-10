package dev.horizon.basepoint.sqlite;

import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.now;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.gt;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.RenderedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
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

  @Test
  @DisplayName("Writes times with six digits of fraction, which compare as text with the times of clients")
  void writeMicroseconds() throws SQLException {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE orders (id INT, created TEXT)");
      statement.execute("INSERT INTO orders VALUES (1, '2026-01-05T10:00:00.123456Z')");
      RenderedStatement query = orders.query().select("now", now()).select("later", add(column("created"),
          Duration.ofDays(1))).build();
      try (PreparedStatement prepared = connection.prepareStatement(query.text());
          ResultSet row = prepared.executeQuery()) {
        assertThat(row.next()).isTrue();
        assertThat(row.getString("now")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}000Z");
        assertThat(row.getString("later")).isEqualTo("2026-01-06T10:00:00.123000Z");
      }
      RenderedStatement later = orders.query().columns("id").where(gt(add(column("created"), Duration.ofDays(1)),
          "2026-01-06T10:00:00.122999Z")).build();
      try (PreparedStatement prepared = connection.prepareStatement(later.text())) {
        prepared.setObject(1, later.params().getFirst());
        try (ResultSet row = prepared.executeQuery()) {
          assertThat(row.next()).as("a time after one a microsecond earlier").isTrue();
        }
      }
    }
  }
}
