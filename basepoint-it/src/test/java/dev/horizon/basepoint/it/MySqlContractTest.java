package dev.horizon.basepoint.it;

class MySqlContractTest extends ContractTest {

  @Override
  protected Database database() {
    return new MySqlDatabase();
  }
}
