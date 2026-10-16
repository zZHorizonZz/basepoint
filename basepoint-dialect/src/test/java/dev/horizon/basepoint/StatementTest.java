package dev.horizon.basepoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.Clause.DeleteFrom;
import dev.horizon.basepoint.Clause.From;
import dev.horizon.basepoint.Clause.InsertInto;
import dev.horizon.basepoint.Clause.Limit;
import dev.horizon.basepoint.Clause.Offset;
import dev.horizon.basepoint.Clause.OrderBy;
import dev.horizon.basepoint.Clause.Select;
import dev.horizon.basepoint.Clause.Set;
import dev.horizon.basepoint.Clause.Update;
import dev.horizon.basepoint.Clause.Where;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StatementTest {

  private final Select select = new Select(false, List.of());
  private final From from = new From();
  private final Limit limit = new Limit(10);
  private final Statement statement = new Statement(Source.aliased("orders"), List.of(select, from, limit));

  @Nested
  class Find {

    @Test
    void returnsTheFirstClauseOfAType() {
      assertThat(statement.find(Limit.class)).containsSame(limit);
    }

    @Test
    void returnsEmptyForAMissingType() {
      assertThat(statement.find(Offset.class)).isEmpty();
      assertThat(statement.has(Offset.class)).isFalse();
    }
  }

  @Nested
  class Narrowing {

    @Test
    void joinsTheFilterOfAWhereClause() {
      Where where = new Where(Filter.eq("state", "OPEN"));
      Statement filtered = new Statement(Source.aliased("orders"), List.of(select, from, where, limit));

      assertThat(filtered.where(Filter.eq("id", 1)).clauses()).containsExactly(select, from,
          new Where(Filter.and(Filter.eq("state", "OPEN"), Filter.eq("id", 1))), limit);
    }

    @Test
    void addsAWhereClauseAfterTheFrom() {
      assertThat(statement.where(Filter.eq("id", 1)).clauses()).containsExactly(select, from,
          new Where(Filter.eq("id", 1)), limit);
    }

    @Test
    void addsAWhereClauseAfterTheSetOrTheDeleteFrom() {
      Set set = new Set(List.of());
      Statement update = new Statement(Source.named("orders"), List.of(new Update(), set));
      Statement delete = new Statement(Source.named("orders"), List.of(new DeleteFrom()));

      assertThat(update.where(Filter.eq("id", 1)).clauses()).containsExactly(new Update(), set,
          new Where(Filter.eq("id", 1)));
      assertThat(delete.where(Filter.eq("id", 1)).clauses()).containsExactly(new DeleteFrom(),
          new Where(Filter.eq("id", 1)));
    }

    @Test
    void rejectsAStatementWithoutRowsToFilter() {
      Statement insert = new Statement(Source.named("orders"), List.of(new InsertInto(List.of("id"))));

      assertThatThrownBy(() -> insert.where(Filter.eq("id", 1))).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class Changes {

    @Test
    void withAddsAtTheEnd() {
      Offset offset = new Offset(5);

      assertThat(statement.with(offset).clauses()).containsExactly(select, from, limit, offset);
    }

    @Test
    void replaceSubstitutesClauses() {
      Offset offset = new Offset(5);

      assertThat(statement.replace(limit, offset, limit).clauses()).containsExactly(select, from, offset, limit);
    }

    @Test
    void removeDropsTheClause() {
      assertThat(statement.remove(limit).clauses()).containsExactly(select, from);
    }

    @Test
    void beforeInsertsBeforeTheAnchor() {
      OrderBy order = new OrderBy(List.of());

      assertThat(statement.before(Limit.class, order).clauses()).containsExactly(select, from, order, limit);
    }

    @Test
    void afterInsertsAfterTheAnchor() {
      Offset offset = new Offset(5);

      assertThat(statement.after(From.class, offset).clauses()).containsExactly(select, from, offset, limit);
    }

    @Test
    void beforeAndAfterAddAtTheEndWithoutTheAnchor() {
      OrderBy order = new OrderBy(List.of());

      assertThat(statement.before(Offset.class, order).clauses()).endsWith(order);
      assertThat(statement.after(Offset.class, order).clauses()).endsWith(order);
    }

    @Test
    void leavesTheOriginalUnchanged() {
      statement.with(new Offset(5));
      statement.remove(limit);

      assertThat(statement.clauses()).containsExactly(select, from, limit);
    }

    @Test
    void rejectsAClauseThatIsNotInTheStatement() {
      assertThatThrownBy(() -> statement.remove(new Limit(10))).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("does not contain");
    }
  }

  @Nested
  class SelectColumns {

    @Test
    void keepsAllColumnsWhenAColumnIsAdded() {
      Select more = select.with(Expression.column("extra"));

      assertThat(more.allColumns()).isTrue();
      assertThat(more.columns()).containsExactly(new Expression.AllColumns(), Expression.column("extra"));
    }

    @Test
    void namedColumnsAreNotAllColumns() {
      assertThat(new Select(false, List.of(Expression.column("id"))).allColumns()).isFalse();
    }
  }
}
