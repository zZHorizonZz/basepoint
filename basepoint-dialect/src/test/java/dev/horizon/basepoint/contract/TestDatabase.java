package dev.horizon.basepoint.contract;

import dev.horizon.basepoint.Dialect;
import java.sql.Connection;
import java.sql.SQLException;

public interface TestDatabase {

  Dialect dialect();

  Connection connect() throws SQLException;

  boolean returnsUpdatedRows() throws SQLException;

  boolean includesWholeRows();
}
