package dev.horizon.basepoint;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecoratorTest {

  private final Node limit = new Clause.Limit(10);
  private final Node offset = new Clause.Offset(5);

  @Test
  void ofRewritesOnlyNodesOfItsType() {
    Decorator decorator = Decorator.of(Clause.Limit.class, ignored -> offset);

    assertThat(decorator.decorate(limit)).isSameAs(offset);
    assertThat(decorator.decorate(offset)).isSameAs(offset);
  }

  @Test
  void thenAppliesBothInOrder() {
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
  void noneReturnsTheSameNode() {
    assertThat(Decorator.none().decorate(limit)).isSameAs(limit);
  }

  @Test
  void writerDecoratesEveryNodeOnce() {
    List<Node> seen = new ArrayList<>();
    Decorator recorder = node -> {
      seen.add(node);
      return node;
    };
    Dialect dialect = new StandardDialect(recorder);

    dialect.collection("orders").query().where(eq("id", 1)).build();

    assertThat(seen).hasAtLeastOneElementOfType(Statement.class).hasAtLeastOneElementOfType(Clause.Where.class)
        .hasAtLeastOneElementOfType(Filter.Compare.class).hasAtLeastOneElementOfType(Expression.Value.class);
    assertThat(seen).doesNotHaveDuplicates();
  }

  @Test
  void applicationDecoratorRunsBeforeTheDialectDecorator() {
    List<String> calls = new ArrayList<>();
    Dialect dialect = new StandardDialect(Decorator.of(Statement.class, statement -> {
      calls.add("dialect");
      return statement;
    })).decorated(Decorator.of(Statement.class, statement -> {
      calls.add("application");
      return statement;
    }));

    dialect.collection("orders").query().build();

    assertThat(calls).containsExactly("application", "dialect");
  }
}
