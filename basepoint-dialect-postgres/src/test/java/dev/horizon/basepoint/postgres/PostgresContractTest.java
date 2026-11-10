package dev.horizon.basepoint.postgres;

import dev.horizon.basepoint.contract.DialectContract;

class PostgresContractTest extends DialectContract {

  PostgresContractTest() {
    super(new PostgresTestDatabase());
  }
}
