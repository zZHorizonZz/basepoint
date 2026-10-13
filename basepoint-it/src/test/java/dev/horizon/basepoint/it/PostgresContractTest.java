package dev.horizon.basepoint.it;

class PostgresContractTest extends ContractTest {

  @Override
  protected Database database() {
    return new PostgresDatabase();
  }
}
