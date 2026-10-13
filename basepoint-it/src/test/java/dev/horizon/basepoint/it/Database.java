package dev.horizon.basepoint.it;

import dev.horizon.basepoint.Dialect;
import java.sql.Connection;
import java.sql.SQLException;

/** A database server of the tests, and the dialect that writes its statements for JDBC. */
public interface Database {

  Dialect dialect();

  Connection connect() throws SQLException;

  /** Tells if the database makes JSON of a whole row, so that an include needs no columns. */
  boolean jsonOfRows();

  /** Tells if the database gives the rows of a write. */
  boolean returning();
}
