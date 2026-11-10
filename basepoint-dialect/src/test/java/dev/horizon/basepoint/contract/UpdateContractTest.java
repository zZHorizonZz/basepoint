package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.multiply;
import static dev.horizon.basepoint.expression.Expression.value;
import static dev.horizon.basepoint.filter.Filter.eq;
import static dev.horizon.basepoint.filter.Filter.exists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.basepoint.UpdateQuery;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class UpdateContractTest extends ContractTest {

  @Test
  @DisplayName("Sets, increments and unsets columns of the rows that the filter keeps")
  void update() throws SQLException {
    assertThat(execute(orders.update().set("state", "CLOSED").increment("total", 5).unset("note")
        .where(eq("id", 2)))).isEqualTo(1);

    Map<String, Object> row = rows(orders.query().where(eq("id", 2))).getFirst();
    assertThat(row).containsEntry("state", "CLOSED").containsEntry("note", null);
    assertThat(number(row.get("total"))).isEqualTo(85);
  }

  @Test
  @DisplayName("Sets a column to the value of an expression of the row")
  void updateByExpression() throws SQLException {
    assertThat(execute(orders.update().set("total", multiply(column("total"), value(2))).where(eq("id", 1))))
        .isEqualTo(1);
    assertThat(number(rows(orders.query().where(eq("id", 1))).getFirst().get("total"))).isEqualTo(300);
  }

  @Test
  @DisplayName("Keeps the last value of a column that is set twice")
  void setTwice() throws SQLException {
    execute(orders.update().set("state", "A").set("state", "B").where(eq("id", 1)));

    assertThat(rows(orders.query().where(eq("id", 1))).getFirst()).containsEntry("state", "B");
  }

  @Test
  @DisplayName("Updates the rows with related rows")
  void updateRelated() throws SQLException {
    assertThat(execute(orders.update().set("note", "big").where(exists(items.query().on("order_id", "id")
        .where(eq("sku", "MUG")))))).isEqualTo(2);
    assertThat(ids(orders.query().where(eq("note", "big")))).containsExactly(1, 2);
  }

  @Test
  @DisplayName("Updates the rows that filter text keeps")
  void updateByText() throws SQLException {
    assertThat(execute(orders.update().set("note", "eu").where("region = EU"))).isEqualTo(2);
    assertThat(ids(orders.query().where(eq("note", "eu")))).containsExactly(1, 3);
  }

  @Test
  @DisplayName("Updates every row with all()")
  void updateAll() throws SQLException {
    assertThat(execute(orders.update().set("note", "x").all())).isEqualTo(5);
  }

  @Test
  @DisplayName("Returns the updated rows as they are after the update")
  void returnUpdated() throws SQLException {
    assumeTrue(database.returnsUpdatedRows());
    UpdateQuery update = orders.update().increment("total", 1).where("state = OPEN").returning("id", "total");

    assertThat(rows(update)).extracting(row -> number(row.get("id")), row -> number(row.get("total")))
        .containsExactlyInAnyOrder(tuple(1L, 151L), tuple(2L, 81L), tuple(5L, 221L));
  }

  @Test
  @DisplayName("Fails with a syntax error where the database cannot return updated rows")
  void refuseReturning() throws SQLException {
    assumeFalse(database.returnsUpdatedRows());
    UpdateQuery update = orders.update().increment("total", 1).where("state = OPEN").returning("id", "total");

    assertThatThrownBy(() -> rows(update)).isInstanceOf(SQLSyntaxErrorException.class)
        .hasMessageContaining("RETURNING");
  }
}
