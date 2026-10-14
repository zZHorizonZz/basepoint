package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Combinable;
import dev.horizon.basepoint.CompoundQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.Source;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The default {@link CompoundQuery}.
 *
 * <p>
 * The operators apply from left to right. An operand becomes a derived table ({@code SELECT * FROM (...) AS t})
 * when it has its own limit or offset, or when it is a compound query that would otherwise group differently. A
 * compound query with its own sort, limit or offset also becomes a derived table.
 */
public final class CompoundQueryImpl implements CompoundQuery {

  /** A set operator. */
  public enum Operator {
    UNION_ALL("UNION ALL"),
    UNION("UNION"),
    INTERSECT("INTERSECT"),
    EXCEPT("EXCEPT");

    private final String keyword;

    Operator(String keyword) {
      this.keyword = keyword;
    }
  }

  private final Combinable left;
  private final Operator operator;
  private final Combinable right;
  private final List<Clause.Sort> order;
  private final Long limit;
  private final Long offset;

  private CompoundQueryImpl(Combinable left, Operator operator, Combinable right, List<Clause.Sort> order, Long limit,
      Long offset) {
    this.left = left;
    this.operator = operator;
    this.right = right;
    this.order = order;
    this.limit = limit;
    this.offset = offset;
  }

  public CompoundQueryImpl(Combinable left, Operator operator, Combinable right) {
    this(left, operator, right, List.of(), null, null);
  }

  @Override
  public CompoundQuery distinct() {
    return operator != Operator.UNION_ALL ? this
        : new CompoundQueryImpl(left, Operator.UNION, right, order, limit, offset);
  }

  @Override
  public CompoundQuery orderBy(String column) {
    return orderBy(column, Direction.ASC);
  }

  @Override
  public CompoundQuery orderBy(String column, Direction direction) {
    List<Clause.Sort> more = new ArrayList<>(order);
    more.add(new Clause.Sort(Expression.column(column), direction));
    return new CompoundQueryImpl(left, operator, right, List.copyOf(more), limit, offset);
  }

  @Override
  public CompoundQuery limit(long rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("The limit must be greater than 0, but is " + rows + ".");
    }
    return new CompoundQueryImpl(left, operator, right, order, rows, offset);
  }

  @Override
  public CompoundQuery offset(long rows) {
    if (rows < 0) {
      throw new IllegalArgumentException("The offset must be 0 or greater, but is " + rows + ".");
    }
    return new CompoundQueryImpl(left, operator, right, order, limit, rows);
  }

  @Override
  public CompoundQuery union(Combinable other) {
    return new CompoundQueryImpl(this, Operator.UNION_ALL, other);
  }

  @Override
  public CompoundQuery intersect(Combinable other) {
    return new CompoundQueryImpl(this, Operator.INTERSECT, other);
  }

  @Override
  public CompoundQuery except(Combinable other) {
    return new CompoundQueryImpl(this, Operator.EXCEPT, other);
  }

  @Override
  public Statement statement() {
    Statement set = new Statement(null, List.of(new Clause.Operand(operand(left, true)),
        new Clause.SetOperator(operator.keyword), new Clause.Operand(operand(right, false))));
    if (!shaped()) {
      return set;
    }
    List<Clause> clauses = new ArrayList<>(List.of(new Clause.Select(false, List.of()), new Clause.From()));
    if (!order.isEmpty()) {
      clauses.add(new Clause.OrderBy(order));
    }
    if (limit != null) {
      clauses.add(new Clause.Limit(limit));
    }
    if (offset != null) {
      clauses.add(new Clause.Offset(offset));
    }
    return new Statement(Source.of(set), clauses);
  }

  @Override
  public RenderedStatement build() {
    return new StatementWriter(dialect()).render(statement()).rendered();
  }

  private Dialect dialect() {
    return switch (left) {
      case QueryImpl query -> query.dialect();
      case CompoundQueryImpl compound -> compound.dialect();
      default -> throw unknown(left);
    };
  }

  private boolean shaped() {
    return !order.isEmpty() || limit != null || offset != null;
  }

  private Statement operand(Combinable read, boolean leftSide) {
    return switch (read) {
      case QueryImpl query when !query.paged() -> query.derived();
      case CompoundQueryImpl compound when leftSide && compound.operator == operator && !compound.shaped() ->
        compound.statement();
      case QueryImpl query -> table(query.statement());
      case CompoundQueryImpl compound -> table(compound.statement());
      default -> throw unknown(read);
    };
  }

  /** Wraps a statement in {@code SELECT * FROM (statement) AS t}. */
  private static Statement table(Statement statement) {
    return new Statement(Source.of(statement), List.of(new Clause.Select(false, List.of()), new Clause.From()));
  }

  private static IllegalArgumentException unknown(Combinable read) {
    return new IllegalArgumentException("A compound query accepts only queries from a collection, not "
        + read.getClass().getName() + ".");
  }
}
