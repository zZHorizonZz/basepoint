package dev.horizon.basepoint.postgres;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class PostgresTestDatabase implements TestDatabase {

  private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
      System.getProperty("basepoint.test.postgres.image", "postgres:17-alpine"));

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
  public boolean returnsUpdatedRows() {
    return true;
  }

  @Override
  public boolean includesWholeRows() {
    return true;
  }

  @Override
  public String jsonType() {
    return "JSONB";
  }
}
