package dev.horizon.basepoint.it;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.sqlserver.SqlServerDialect;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

public final class SqlServerDatabase implements Database {
  private static final MSSQLServerContainer CONTAINER = new MSSQLServerContainer(
      System.getProperty("basepoint.it.sqlserver.image", "mcr.microsoft.com/mssql/server:2022-latest")).acceptLicense();

  @Override
  public Dialect dialect() {
    return new SqlServerDialect(ParameterStyle.QUESTION_MARK);
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
