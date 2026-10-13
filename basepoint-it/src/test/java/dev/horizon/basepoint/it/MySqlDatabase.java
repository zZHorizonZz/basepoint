package dev.horizon.basepoint.it;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.mysql.MySqlDialect;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.mysql.MySQLContainer;

public final class MySqlDatabase implements Database {
  private static final MySQLContainer CONTAINER = new MySQLContainer(
      System.getProperty("basepoint.it.mysql.image", "mysql:8.4"));

  @Override
  public Dialect dialect() {
    return new MySqlDialect();
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
  public boolean jsonOfRows() {
    return false;
  }

  @Override
  public boolean returning() {
    return false;
  }
}
