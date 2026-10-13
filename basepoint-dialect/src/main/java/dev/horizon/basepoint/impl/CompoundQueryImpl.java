package dev.horizon.basepoint.impl;

import dev.horizon.basepoint.Combinable;
import dev.horizon.basepoint.CompoundQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Direction;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;

import dev.horizon.basepoint.StatementWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The rows of two reads that a set operator joins. Each step gives a new compound query.
 *
 * <p>
 * The reads join from left to right, as the calls read. A read that orders or limits its own rows, and a compound
 * query that would join in another order than the calls read, is written as a table of its own, which every database
 * reads in the same way. An order or a limit of the compound query is of a table of its rows.
 */
public final class CompoundQueryImpl implements CompoundQuery {

  /** How the rows of two reads join. */
  enum Operator {
    UNION_ALL("UNION ALL"),
    UNION("UNION"),
    INTERSECT("INTERSECT"),
    EXCEPT("EXCEPT");

    private final String sql;

    Operator(String sql) {
      this.sql = sql;
    }
  }

  private final Combinable left;
  private final Operator operator;
  private final Combinable right;
  private final List<Sort> order;
  private final Long limit;
  private final Long offset;

  private CompoundQueryImpl(Combinable left, Operator operator, Combinable right, List<Sort> order, Long limit,
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

  /**
   * An order of the rows.
   *
   * @param column the name of the column
   * @param direction the direction
   */
  private record Sort(String column, Direction direction) {
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
    List<Sort> more = new ArrayList<>(order);
    more.add(new Sort(column, direction));
    return new CompoundQueryImpl(left, operator, right, List.copyOf(more), limit, offset);
  }

  @Override
  public CompoundQuery limit(long rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("the limit must be positive, but is " + rows);
    }
    return new CompoundQueryImpl(left, operator, right, order, rows, offset);
  }

  @Override
  public CompoundQuery offset(long rows) {
    if (rows < 0) {
      throw new IllegalArgumentException("the offset must not be negative, but is " + rows);
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
  public Statement build() {
    StatementWriter sql = new StatementWriter(dialect());
    write(sql);
    return sql.toStatement();
  }

  private Dialect dialect() {
    return switch (left) {
      case QueryImpl query -> query.dialect();
      case CompoundQueryImpl compound -> compound.dialect();
      default -> throw unknown(left);
    };
  }

  private void write(StatementWriter sql) {
    if (!shaped()) {
      writeSet(sql);
      return;
    }
    String alias = sql.alias();
    sql.append("SELECT ");
    sql.dialect().paging().top(sql, limit, offset);
    sql.append("* FROM (");
    writeSet(sql);
    sql.append(") AS ").append(alias);
    sql.scoped(alias, () -> {
      if (!order.isEmpty()) {
        sql.append(" ORDER BY ");
        for (int i = 0; i < order.size(); i++) {
          sql.append(i == 0 ? "" : ", ").column(order.get(i).column())
              .append(order.get(i).direction() == Direction.DESC ? " DESC" : " ASC");
        }
      }
      if (limit != null || offset != null) {
        sql.dialect().paging().paging(sql, limit, offset, !order.isEmpty());
      }
    });
  }

  private boolean shaped() {
    return !order.isEmpty() || limit != null || offset != null;
  }

  private void writeSet(StatementWriter sql) {
    operand(sql, left, true);
    sql.append(" " + operator.sql + " ");
    operand(sql, right, false);
  }

  private void operand(StatementWriter sql, Combinable read, boolean leftSide) {
    switch (read) {
      case QueryImpl query when !query.paged() -> query.writeDerived(sql);
      case CompoundQueryImpl compound when leftSide && compound.operator == operator && !compound.shaped() ->
        compound.write(sql);
      case QueryImpl query -> table(sql, () -> query.writeStatement(sql));
      case CompoundQueryImpl compound -> table(sql, () -> compound.write(sql));
      default -> throw unknown(read);
    }
  }

  private void table(StatementWriter sql, Runnable read) {
    String alias = sql.alias();
    sql.append("SELECT * FROM (");
    read.run();
    sql.append(") AS ").append(alias);
  }

  private static IllegalArgumentException unknown(Combinable read) {
    return new IllegalArgumentException("a compound query joins the queries of collections, not "
        + read.getClass().getName());
  }
}
