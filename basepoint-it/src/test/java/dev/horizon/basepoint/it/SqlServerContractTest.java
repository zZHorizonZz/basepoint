package dev.horizon.basepoint.it;

class SqlServerContractTest extends ContractTest {

  @Override
  protected Database database() {
    return new SqlServerDatabase();
  }
}
