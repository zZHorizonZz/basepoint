package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.LockMode;
import java.sql.SQLException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class LockContractTest extends ContractTest {

  @ParameterizedTest
  @EnumSource(LockMode.class)
  void readsTheRowsThatItLocks(LockMode mode) throws SQLException {
    assertThat(ids(orders.query().where(eq("state", "OPEN")).lock(mode))).containsExactly(1, 2, 5);
  }

  @ParameterizedTest
  @EnumSource(LockMode.class)
  void locksALimitedQuery(LockMode mode) throws SQLException {
    assertThat(idsInOrder(orders.query().orderBy("id").limit(2).lock(mode))).containsExactly(1, 2);
  }
}
