package dev.horizon.basepoint;

import static dev.horizon.basepoint.filter.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.expression.Value;
import dev.horizon.basepoint.filter.Compare;
import dev.horizon.basepoint.statement.Decorator;
import dev.horizon.basepoint.statement.From;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Node;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Where;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecoratorTest {

  private final Node limit = new Limit(10);
  private final Node offset = new Offset(5);

  @Test
  @DisplayName("Rewrites only the nodes of its type")
  void rewriteType() {
    Decorator decorator = Decorator.of(Limit.class, node -> offset);

    assertThat(decorator.decorate(limit)).isSameAs(offset);
    assertThat(decorator.decorate(offset)).isSameAs(offset);
  }

  @Test
  @DisplayName("Applies two decorators in order")
  void chain() {
    List<String> calls = new ArrayList<>();
    Decorator first = node -> {
      calls.add("first");
      return node;
    };
    Decorator second = node -> {
      calls.add("second");
      return node;
    };

    first.then(second).decorate(limit);

    assertThat(calls).containsExactly("first", "second");
  }

  @Test
  @DisplayName("Leaves a node alone with none()")
  void none() {
    assertThat(Decorator.none().decorate(limit)).isSameAs(limit);
  }

  @Test
  @DisplayName("Decorates every node of a statement once")
  void decorateOnce() {
    List<Node> seen = new ArrayList<>();
    Decorator recorder = node -> {
      seen.add(node);
      return node;
    };

    new StandardDialect(recorder).collection("orders").query().where(eq("id", 1)).build();

    assertThat(seen).hasAtLeastOneElementOfType(Statement.class).hasAtLeastOneElementOfType(Where.class)
        .hasAtLeastOneElementOfType(Compare.class).hasAtLeastOneElementOfType(Value.class).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("Applies the decorator of the application before the decorator of the dialect")
  void applicationFirst() {
    List<String> calls = new ArrayList<>();
    Dialect dialect = new StandardDialect(Decorator.of(Statement.class, statement -> {
      calls.add("dialect");
      return statement;
    })).decorate(Decorator.of(Statement.class, statement -> {
      calls.add("application");
      return statement;
    }));

    dialect.collection("orders").query().build();

    assertThat(calls).containsExactly("application", "dialect");
  }

  @Test
  @DisplayName("Gives the statement of a query before any decorator rewrites it")
  void treeFirst() {
    Dialect dialect = new StandardDialect(Decorator.of(Statement.class, statement -> statement.remove(statement.find(
        Limit.class).orElseThrow())));

    assertThat(dialect.collection("orders").query().where(eq("id", 1)).limit(1).statement().clauses())
        .extracting(Object::getClass).containsExactly(Select.class, From.class, Where.class, Limit.class);
  }
}
