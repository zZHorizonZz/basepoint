package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.eq;
import static dev.horizon.basepoint.Filter.notExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.DeleteQuery;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public abstract class DeleteContractTest extends ContractTest {

  @Test
  void deletesMatchingRows() throws SQLException {
    assertThat(execute(orders.delete().where(eq("state", "OPEN")))).isEqualTo(3);

    assertThat(ids(orders.query())).containsExactly(3, 4);
  }

  @Test
  void deletesRowsWithoutRelatedRows() throws SQLException {
    assertThat(execute(orders.delete().where(notExists(items.query().on("order_id", "id"))))).isEqualTo(1);

    assertThat(ids(orders.query())).containsExactly(1, 2, 3, 5);
  }

  @Test
  void deletesEveryRowOnlyWithAll() throws SQLException {
    assertThatThrownBy(() -> orders.delete().build()).isInstanceOf(IllegalStateException.class);

    assertThat(execute(orders.delete().all())).isEqualTo(5);
  }

  @Test
  void returnsTheDeletedRows() throws SQLException {
    DeleteQuery delete = items.delete().where(eq("order_id", 1)).returning("id");

    assertThat(sortedIds(delete)).containsExactly(10, 11, 12);
  }
}
