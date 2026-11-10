package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class DecoratorContractTest extends ContractTest {

  private final Decorator europe = Decorator.of(Statement.class, statement -> {
    if (!(statement.source() instanceof Source.Table(String table) && table.equals("orders")
        && statement.has(Select.class))) {
      return statement;
    }
    return statement.where(eq("region", "EU"));
  });

  private CollectionReference europeanOrders() {
    return dialect.decorate(europe).collection("orders");
  }

  @Test
  @DisplayName("Adds a filter to a query without one")
  void addFilter() throws SQLException {
    assertThat(ids(europeanOrders().query())).containsExactly(1, 3);
  }

  @Test
  @DisplayName("Adds a filter to the filter of the query")
  void extendFilter() throws SQLException {
    assertThat(ids(europeanOrders().query().where(eq("state", "OPEN")))).containsExactly(1);
  }

  @Test
  @DisplayName("Filters before the dialect writes the limit")
  void runFirst() throws SQLException {
    assertThat(idsInOrder(europeanOrders().query().orderBy("id", Direction.DESC).limit(1))).containsExactly(3);
  }

  @Test
  @DisplayName("Leaves the statements of other tables as they are")
  void leaveOthers() throws SQLException {
    assertThat(ids(dialect.decorate(europe).collection("archived").query())).containsExactly(1, 6, 7);
  }
}
