package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Filter.and;
import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Filter;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public abstract class DecoratorContractTest extends ContractTest {

  private static final Decorator EUROPE = Decorator.of(Statement.class, statement -> {
    if (!(statement.source() != null && statement.source().rows() instanceof Source.Table table
        && table.name().equals("orders") && statement.has(Clause.Select.class))) {
      return statement;
    }
    Filter region = eq("region", "EU");
    return statement.find(Clause.Where.class)
        .map(where -> statement.replace(where, new Clause.Where(and(where.filter(), region))))
        .orElseGet(() -> statement.after(Clause.From.class, new Clause.Where(region)));
  });

  @Test
  void addsAWhereClause() throws SQLException {
    assertThat(ids(europeanOrders().query())).containsExactly(1, 3);
  }

  @Test
  void extendsAWhereClause() throws SQLException {
    assertThat(ids(europeanOrders().query().where(eq("state", "OPEN")))).containsExactly(1);
  }

  @Test
  void runsBeforeTheDialectDecorator() throws SQLException {
    assertThat(idsInOrder(europeanOrders().query().orderBy("id").limit(1))).containsExactly(1);
  }

  private CollectionReference europeanOrders() {
    return dialect.decorated(EUROPE).collection("orders");
  }
}
