package dev.horizon.basepoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.basepoint.expression.AllColumns;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.filter.Filter;
import dev.horizon.basepoint.statement.Assignments;
import dev.horizon.basepoint.statement.Clause;
import dev.horizon.basepoint.statement.DeleteFrom;
import dev.horizon.basepoint.statement.From;
import dev.horizon.basepoint.statement.GroupBy;
import dev.horizon.basepoint.statement.InsertInto;
import dev.horizon.basepoint.statement.Join;
import dev.horizon.basepoint.statement.Limit;
import dev.horizon.basepoint.statement.Offset;
import dev.horizon.basepoint.statement.OrderBy;
import dev.horizon.basepoint.statement.Returning;
import dev.horizon.basepoint.statement.Select;
import dev.horizon.basepoint.statement.Source;
import dev.horizon.basepoint.statement.Statement;
import dev.horizon.basepoint.statement.Update;
import dev.horizon.basepoint.statement.Where;
import dev.horizon.basepoint.statement.With;
import dev.horizon.basepoint.statement.WithQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StatementTest {

  private final Select select = new Select(false, List.of());
  private final From from = new From();
  private final Limit limit = new Limit(10);
  private final Statement statement = new Statement(new Source.Table("orders"), List.of(select, from, limit));
  private final List<Class<? extends Clause>> order = List.of(Select.class, From.class, Join.class, Where.class,
      GroupBy.class, OrderBy.class, Limit.class, Offset.class);

  @Nested
  @DisplayName("Find")
  class Find {

    @Test
    @DisplayName("Finds the first clause of a type")
    void first() {
      assertThat(statement.find(Limit.class)).containsSame(limit);
    }

    @Test
    @DisplayName("Finds nothing for a type that the statement does not have")
    void missing() {
      assertThat(statement.find(Offset.class)).isEmpty();
      assertThat(statement.has(Offset.class)).isFalse();
    }

    @Test
    @DisplayName("Finds every clause of a type, in order")
    void all() {
      Join first = new Join("a", "x", Filter.eq("id", 1), false);
      Join second = new Join("b", "y", Filter.eq("id", 2), false);

      assertThat(new Statement(new Source.Table("orders"), List.of(select, from, first, second)).findAll(Join.class))
          .containsExactly(first, second);
    }
  }

  @Nested
  @DisplayName("Where")
  class Narrowing {

    @Test
    @DisplayName("Joins a filter to the filter of the WHERE clause")
    void join() {
      Where where = new Where(Filter.eq("state", "OPEN"));
      Statement withWhere = new Statement(new Source.Table("orders"), List.of(select, from, where, limit));

      assertThat(withWhere.where(Filter.eq("id", 1)).clauses()).containsExactly(select, from,
          new Where(Filter.and(Filter.eq("state", "OPEN"), Filter.eq("id", 1))), limit);
    }

    @Test
    @DisplayName("Adds a WHERE clause after the FROM and the joins")
    void addAfterJoins() {
      Join join = new Join("customer", "customers", Filter.eq("id", 1), false);
      Statement withJoin = new Statement(new Source.Table("orders"), List.of(select, from, join, limit));

      assertThat(withJoin.where(Filter.eq("id", 1)).clauses()).containsExactly(select, from, join,
          new Where(Filter.eq("id", 1)), limit);
    }

    @Test
    @DisplayName("Adds a WHERE clause after the SET or the DELETE FROM, and before RETURNING")
    void addToWrites() {
      Assignments assignments = new Assignments(List.of());
      Returning returning = new Returning(List.of());
      Statement update = new Statement(new Source.Target("orders"), List.of(new Update(), assignments, returning));
      Statement delete = new Statement(new Source.Target("orders"), List.of(new DeleteFrom()));

      assertThat(update.where(Filter.eq("id", 1)).clauses()).containsExactly(new Update(), assignments,
          new Where(Filter.eq("id", 1)), returning);
      assertThat(delete.where(Filter.eq("id", 1)).clauses()).containsExactly(new DeleteFrom(),
          new Where(Filter.eq("id", 1)));
    }

    @Test
    @DisplayName("Refuses a filter of a statement without rows to filter")
    void refuse() {
      Statement insert = new Statement(new Source.Target("orders"), List.of(new InsertInto(List.of("id"))));

      assertThatThrownBy(() -> insert.where(Filter.eq("id", 1))).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Only a SELECT, an UPDATE or a DELETE has a filter");
    }
  }

  @Nested
  @DisplayName("Changes")
  class Changes {

    @Test
    @DisplayName("Adds a clause at the end")
    void append() {
      Offset offset = new Offset(5);

      assertThat(statement.with(offset).clauses()).containsExactly(select, from, limit, offset);
    }

    @Test
    @DisplayName("Replaces a clause that equals one of the statement, not only the same instance")
    void replaceEqual() {
      assertThat(statement.replace(new Limit(10), new Limit(5)).clauses()).containsExactly(select, from,
          new Limit(5));
    }

    @Test
    @DisplayName("Replaces a clause with several clauses")
    void replaceMany() {
      Offset offset = new Offset(5);

      assertThat(statement.replace(limit, offset, limit).clauses()).containsExactly(select, from, offset, limit);
    }

    @Test
    @DisplayName("Removes a clause")
    void remove() {
      assertThat(statement.remove(limit).clauses()).containsExactly(select, from);
    }

    @Test
    @DisplayName("Inserts a clause before or after the first clause of a type, or at the end without one")
    void insertByAnchor() {
      OrderBy sort = new OrderBy(List.of());
      Offset offset = new Offset(5);

      assertThat(statement.before(Limit.class, sort).clauses()).containsExactly(select, from, sort, limit);
      assertThat(statement.after(From.class, offset).clauses()).containsExactly(select, from, offset, limit);
      assertThat(statement.before(Offset.class, sort).clauses()).endsWith(sort);
    }

    @Test
    @DisplayName("Puts a clause in the place of its type in the order of SQL")
    void put() {
      OrderBy sort = new OrderBy(List.of());

      assertThat(statement.put(sort, order).clauses()).containsExactly(select, from, sort, limit);
      assertThat(statement.put(new Limit(3), order).clauses()).containsExactly(select, from, new Limit(3));
      assertThat(statement.put(new Offset(1), order).clauses()).containsExactly(select, from, limit, new Offset(1));
    }

    @Test
    @DisplayName("Adds a clause after the clauses of its type")
    void add() {
      Join first = new Join("a", "x", Filter.eq("id", 1), false);
      Join second = new Join("b", "y", Filter.eq("id", 2), false);

      assertThat(statement.add(first, order).add(second, order).clauses()).containsExactly(select, from, first,
          second, limit);
    }

    @Test
    @DisplayName("Leaves the statement as it was")
    void keep() {
      statement.with(new Offset(5));
      statement.remove(limit);

      assertThat(statement.clauses()).containsExactly(select, from, limit);
    }

    @Test
    @DisplayName("Refuses to remove a clause that the statement does not have")
    void refuseMissing() {
      assertThatThrownBy(() -> statement.remove(new Limit(99))).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("The statement does not contain the clause");
    }

    @Test
    @DisplayName("Refuses to be part of another statement with a WITH query")
    void refuseNesting() {
      Statement with = statement.before(Select.class, new With(List.of(new WithQuery("big", statement, false))));

      assertThatThrownBy(with::requireNestable).isInstanceOf(InvalidQueryException.class)
          .hasMessageContaining("Call with() on the outermost query");
    }
  }

  @Nested
  @DisplayName("Select")
  class SelectColumns {

    @Test
    @DisplayName("Keeps all columns when a column is added to a SELECT of all columns")
    void keepAll() {
      Select more = select.with(Expression.column("extra"));

      assertThat(more.allColumns()).isTrue();
      assertThat(more.columns()).containsExactly(new AllColumns(), Expression.column("extra"));
    }

    @Test
    @DisplayName("Selects only the named columns")
    void named() {
      assertThat(new Select(false, List.of(Expression.column("id"))).allColumns()).isFalse();
    }
  }
}
