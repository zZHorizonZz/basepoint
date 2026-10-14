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
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }

  @Override
  public boolean returnsUpdatedRows() {
    return true;
  }

  @Override
  public boolean includesWholeRows() {
    return false;
  }
}
