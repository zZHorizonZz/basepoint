package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.contract.DialectContract;

class MariaDbContractTest extends DialectContract {

  MariaDbContractTest() {
    super(new MariaDbTestDatabase());
  }
}
