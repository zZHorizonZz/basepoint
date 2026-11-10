package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.testcontainers.mariadb.MariaDBContainer;

final class MariaDbTestDatabase implements TestDatabase {

  private static final MariaDBContainer CONTAINER = new MariaDBContainer(
      System.getProperty("basepoint.test.mariadb.image", "mariadb:13.0"));

  @Override
  public Dialect dialect() {
    return new MariaDbDialect();
  }

  @Override
  public Connection connect() throws SQLException {
    synchronized (CONTAINER) {
      if (!CONTAINER.isRunning()) {
        CONTAINER.start();
      }
    }
    return DriverManager.getConnection(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword());
  }

  @Override
  public Map<String, String> types() {
    return Map.of("json", "JSON", "timestamp", "DATETIME(6)", "text", "TEXT CHARACTER SET utf8mb4", "bool", "BOOLEAN",
        "double", "DOUBLE");
  }

  @Override
  public boolean returnsUpdatedRows() throws SQLException {
    try (Connection connection = connect()) {
      return connection.getMetaData().getDatabaseMajorVersion() >= 13;
    }
  }

  @Override
  public boolean includesWholeRows() {
    return false;
  }

  @Override
  public boolean keepsDecimals() {
    return true;
  }

  @Override
  public boolean locksRows() {
    return true;
  }

  @Override
  public boolean refusedLock(SQLException failure) {
    return failure.getErrorCode() == 1205 || failure.getErrorCode() == 3572;
  }
}
