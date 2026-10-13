package dev.horizon.basepoint.it;

class SqliteContractTest extends ContractTest {

  @Override
  protected Database database() {
    return new SqliteDatabase();
  }
}
