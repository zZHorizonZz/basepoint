package dev.horizon.basepoint.sqlite;

import dev.horizon.basepoint.contract.DialectContract;

class SqliteContractTest extends DialectContract {

  SqliteContractTest() {
    super(new SqliteTestDatabase());
  }
}
