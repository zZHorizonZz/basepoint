package dev.horizon.basepoint.sqlserver;

import dev.horizon.basepoint.contract.DialectContract;

class SqlServerContractTest extends DialectContract {

  SqlServerContractTest() {
    super(new SqlServerTestDatabase());
  }
}
