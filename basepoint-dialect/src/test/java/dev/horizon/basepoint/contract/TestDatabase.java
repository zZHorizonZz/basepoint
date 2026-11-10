package dev.horizon.basepoint.contract;

import dev.horizon.basepoint.Dialect;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface TestDatabase {

  Dialect dialect();

  Connection connect() throws SQLException;

  Map<String, String> types();

  boolean returnsUpdatedRows() throws SQLException;

  boolean includesWholeRows();

  boolean keepsDecimals();

  boolean locksRows();

  boolean refusedLock(SQLException failure);
}
