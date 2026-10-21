package dev.horizon.basepoint.contract;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

final class Fixture {

  private static final List<String> TABLES = List.of("items", "orders", "products", "archived", "defaults", "labelled");

  private static final List<String> SCHEMA = List.of(
      "CREATE TABLE orders (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL, total INT NOT NULL,"
          + " region VARCHAR(10), note VARCHAR(50))",
      "CREATE TABLE items (id INT PRIMARY KEY, order_id INT NOT NULL, sku VARCHAR(20) NOT NULL, qty INT NOT NULL)",
      "CREATE TABLE products (sku VARCHAR(20) PRIMARY KEY, name VARCHAR(50) NOT NULL)",
      "CREATE TABLE archived (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL)",
      "CREATE TABLE defaults (id INT NOT NULL DEFAULT 7, label VARCHAR(10) NOT NULL DEFAULT 'x')");

  private static final String LABELLED = "INSERT INTO labelled (id, labels) VALUES (1, '{\"env\": \"prod\","
      + " \"team\": \"core\"}'), (2, '{\"env\": \"dev\", \"team\": \"web\"}'),"
      + " (3, '{\"env\": \"prod\", \"app.kubernetes.io/name\": \"shop\"}'), (4, '{}'), (5, NULL),"
      + " (6, '{\"replicas\": 3}')";

  private static final List<String> ROWS = List.of(
      "INSERT INTO orders (id, state, total, region, note) VALUES (1, 'OPEN', 150, 'EU', NULL),"
          + " (2, 'OPEN', 80, 'US', 'rush'), (3, 'CLOSED', 300, 'EU', 'gift'), (4, 'CANCELLED', 50, NULL, NULL),"
          + " (5, 'OPEN', 220, 'US', NULL)",
      "INSERT INTO items (id, order_id, sku, qty) VALUES (10, 1, 'MUG', 2), (11, 1, 'CUP', 1),"
          + " (12, 1, 'a%b_c', 3), (13, 2, 'MUG', 5), (14, 3, 'x[1]', 1), (15, 5, 'PLATE', 4)",
      "INSERT INTO products (sku, name) VALUES ('MUG', 'Mug'), ('CUP', 'Cup'), ('PLATE', 'Plate')",
      "INSERT INTO archived (id, state) VALUES (1, 'OPEN'), (6, 'CLOSED'), (7, 'OPEN')");

  private Fixture() {
  }

  static void load(Connection connection, String jsonType) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      for (String table : TABLES) {
        statement.execute("DROP TABLE IF EXISTS " + table);
      }
      for (String sql : SCHEMA) {
        statement.execute(sql);
      }
      for (String sql : ROWS) {
        statement.execute(sql);
      }
      statement.execute("CREATE TABLE labelled (id INT PRIMARY KEY, labels " + jsonType + ")");
      statement.execute(LABELLED);
    }
  }
}
