package dev.horizon.basepoint.it;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.sqlite.SqliteDialect;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SqliteDatabase implements Database {

  @Override
  public Dialect dialect() {
    return new SqliteDialect();
  }

  @Override
  public Connection connect() throws SQLException {
    try {
      Path file = Files.createTempFile("basepoint", ".db");
      return DriverManager.getConnection("jdbc:sqlite:" + file);
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }

  @Override
  public boolean jsonOfRows() {
    return false;
  }

  @Override
  public boolean returning() {
    return true;
  }
}
