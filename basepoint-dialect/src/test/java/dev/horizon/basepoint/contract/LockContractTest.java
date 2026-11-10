package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.basepoint.LockMode;
import dev.horizon.basepoint.RenderedStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class LockContractTest extends ContractTest {

  @ParameterizedTest(name = "{0}")
  @EnumSource(LockMode.class)
  @DisplayName("Reads the rows that it locks")
  void read(LockMode mode) throws SQLException {
    assertThat(ids(orders.query().where(eq("state", "OPEN")).lock(mode))).containsExactly(1, 2, 5);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(LockMode.class)
  @DisplayName("Locks the rows of a sorted and limited query")
  void readPage(LockMode mode) throws SQLException {
    assertThat(idsInOrder(orders.query().orderBy("id").limit(2).lock(mode))).containsExactly(1, 2);
  }

  @Test
  @DisplayName("Makes another transaction that will not wait fail on a locked row")
  void refuseOther() throws SQLException {
    assumeTrue(database.locksRows());
    ids(orders.query().where(eq("id", 1)).lock(LockMode.UPDATE));

    try (Connection rival = database.connect()) {
      SQLException failure = catchThrowableOfType(SQLException.class, () -> readIds(rival, orders.query()
          .where(eq("id", 1)).lock(LockMode.UPDATE_NOWAIT).build()));

      assertThat((Throwable) failure).isNotNull();
      assertThat(database.refusedLock(failure)).as("the error %s refuses a locked row", failure.getMessage())
          .isTrue();
    }
  }

  @Test
  @DisplayName("Lets another transaction skip the locked rows")
  void skipLocked() throws SQLException {
    assumeTrue(database.locksRows());
    ids(orders.query().where(eq("id", 1)).lock(LockMode.UPDATE));

    try (Connection rival = database.connect()) {
      rival.setAutoCommit(false);
      try {
        assertThat(readIds(rival, orders.query().where(eq("state", "OPEN")).lock(LockMode.UPDATE_SKIP_LOCKED)
            .build())).containsExactlyInAnyOrder(2, 5);
      } finally {
        rival.rollback();
      }
    }
  }

  @Test
  @DisplayName("Lets another transaction read a row that is locked for sharing, with a lock for sharing")
  void share() throws SQLException {
    assumeTrue(database.locksRows());
    ids(orders.query().where(eq("id", 1)).lock(LockMode.SHARE));

    try (Connection rival = database.connect()) {
      rival.setAutoCommit(false);
      try {
        assertThat(readIds(rival, orders.query().where(eq("id", 1)).lock(LockMode.SHARE).build()))
            .containsExactly(1);
      } finally {
        rival.rollback();
      }
    }
  }

  @Test
  @DisplayName("Leaves out the lock where the database locks the whole file")
  void dropLock() throws SQLException {
    assumeFalse(database.locksRows());

    assertThat(orders.query().lock(LockMode.UPDATE).build().text()).doesNotContainIgnoringCase("FOR UPDATE");
  }

  private List<Integer> readIds(Connection connection, RenderedStatement statement) throws SQLException {
    try (PreparedStatement jdbc = connection.prepareStatement(statement.text())) {
      for (int i = 0; i < statement.params().size(); i++) {
        jdbc.setObject(i + 1, statement.params().get(i));
      }
      List<Integer> ids = new ArrayList<>();
      try (ResultSet cursor = jdbc.executeQuery()) {
        while (cursor.next()) {
          ids.add(cursor.getInt("id"));
        }
      }
      return ids;
    }
  }
}
