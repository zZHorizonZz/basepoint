package dev.horizon.basepoint.sqlserver;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

final class SqlServerTestDatabase implements TestDatabase {

  private static final MSSQLServerContainer CONTAINER = new MSSQLServerContainer(
      System.getProperty("basepoint.test.sqlserver.image", "mcr.microsoft.com/mssql/server:2022-latest"))
      .acceptLicense();

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
  public Map<String, String> types() {
    return Map.of("json", "NVARCHAR(MAX)", "timestamp", "DATETIME2", "text",
        "NVARCHAR(MAX) COLLATE Latin1_General_100_CI_AS_SC", "bool", "BIT", "double",
        "FLOAT");
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
  public boolean keepsDecimals() {
    return true;
  }

  @Override
  public boolean locksRows() {
    return true;
  }

  @Override
  public boolean refusedLock(SQLException failure) {
    return failure.getErrorCode() == 1222;
  }
}
