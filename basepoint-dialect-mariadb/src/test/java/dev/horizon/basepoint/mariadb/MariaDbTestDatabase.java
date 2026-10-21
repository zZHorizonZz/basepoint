package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
  public String jsonType() {
    return "JSON";
  }
}
