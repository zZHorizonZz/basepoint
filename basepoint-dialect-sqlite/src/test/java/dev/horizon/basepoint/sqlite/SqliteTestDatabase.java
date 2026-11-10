package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.contract.TestDatabase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

final class SqliteTestDatabase implements TestDatabase {

  @Override
  public Dialect dialect() {
    return new SqliteDialect();
  }

  @Override
  public Connection connect() throws SQLException {
    try {
      Path file = Files.createTempFile("basepoint", ".db");
      file.toFile().deleteOnExit();
      return DriverManager.getConnection("jdbc:sqlite:" + file);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  @Override
  public Map<String, String> types() {
    return Map.of("json", "TEXT", "timestamp", "TEXT", "text", "TEXT", "bool", "INTEGER", "double", "REAL");
  }

  @Override
  public boolean returnsUpdatedRows() {
    return true;
  }

  @Override
  public boolean includesWholeRows() {
    return false;
  }

  @Override
  public boolean keepsDecimals() {
    return false;
  }

  @Override
  public boolean locksRows() {
    return false;
  }

  @Override
  public boolean refusedLock(SQLException failure) {
    return false;
  }
}
