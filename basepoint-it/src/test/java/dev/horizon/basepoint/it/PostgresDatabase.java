package dev.horizon.basepoint.it;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.postgres.PostgresDialect;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class PostgresDatabase implements Database {
  private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
      System.getProperty("basepoint.it.postgres.image", "postgres:17-alpine"));

  @Override
  public Dialect dialect() {
    return new PostgresDialect(ParameterStyle.QUESTION_MARK);
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
    return true;
  }

  @Override
  public boolean returning() {
    return true;
  }
}
