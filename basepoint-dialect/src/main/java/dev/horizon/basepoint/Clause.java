package dev.horizon.basepoint;

import java.util.ArrayList;
import java.util.List;

/**
 * A clause of a {@link Statement}.
 *
 * <p>
 * The records in this interface render standard SQL. A dialect decorates the clauses that its database writes
 * differently, and can add clause types of its own.
 */
public interface Clause extends Node {

  /**
   * {@code SELECT} with its column list. An empty list renders all columns of the source, for example
   * {@code "t0".*}.
   *
   * @param distinct {@code true} to render {@code SELECT DISTINCT}
   * @param columns the columns
   */
  record Select(boolean distinct, List<Expression> columns) implements Clause {

    public Select {
      columns = List.copyOf(columns);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append(distinct ? "SELECT DISTINCT " : "SELECT ");
      renderColumns(sql);
    }

    /**
     * Renders the column list, or all columns of the source if the list is empty.
     *
     * @param sql the writer that receives the SQL
     */
    public void renderColumns(StatementWriter sql) {
      sql.render(columns.isEmpty() ? List.of(new Expression.AllColumns()) : columns, ", ");
    }

    /**
     * Returns whether this clause selects all columns of the source.
     *
     * @return {@code true} if the column list is empty or contains {@link Expression.AllColumns}
     */
    public boolean allColumns() {
      return columns.isEmpty() || columns.stream().anyMatch(Expression.AllColumns.class::isInstance);
    }

    /**
     * Returns a copy of this clause with a column added at the end. If this clause selects all columns through an
     * empty list, the copy keeps them with {@link Expression.AllColumns}.
     *
     * @param column the column to add
     * @return the new clause
     */
    public Select with(Expression column) {
      List<Expression> more = new ArrayList<>(columns.isEmpty() ? List.of(new Expression.AllColumns()) : columns);
      more.add(column);
      return new Select(distinct, more);
    }
  }

  /**
   * {@code WITH name AS (rows), ...}, the WITH queries of a statement.
   *
   * @param queries the WITH queries, in their order
   */
  record With(List<WithQuery> queries) implements Clause {

    public With {
      queries = List.copyOf(queries);
    }

    /**
     * Tells if one of the queries reads itself, which makes the whole list recursive.
     *
     * @return true if a query is recursive
     */
    public boolean recursive() {
      return queries.stream().anyMatch(WithQuery::recursive);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append(recursive() ? "WITH RECURSIVE " : "WITH ").render(queries, ", ");
    }
  }

  /**
   * A query of a {@link With}: {@code name AS (rows)}.
   *
   * @param name the name, which the statement reads like a table
   * @param rows the statement of its rows
   * @param recursive true if the rows read the query itself, as {@code start UNION ALL step}
   */
  record WithQuery(String name, Statement rows, boolean recursive) implements Node {

    @Override
    public void render(StatementWriter sql) {
      sql.name(name).append(" AS (").render(rows).append(")");
    }
  }

  /** {@code FROM} with the statement's source and its alias. */
  record From() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("FROM ").render(sql.source()).append(" AS " + sql.qualifier());
    }
  }

  /**
   * The join of a relation: {@code LEFT JOIN table AS alias ON condition}, or {@code JOIN} for an inner join. The
   * condition reads the columns of the table, and the row of the statement as the enclosing row.
   *
   * @param name the name of the relation, which its paths read
   * @param table the table of the relation
   * @param condition the condition of the join
   * @param inner true for an inner join, which keeps only the rows that have a related row
   */
  record Join(String name, String table, Filter condition, boolean inner) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      String alias = sql.relation(name).orElseThrow();
      sql.append(inner ? "JOIN " : "LEFT JOIN ").table(table).append(" AS " + alias + " ON ");
      sql.scoped(alias, new Source.Table(table), () -> sql.render(condition));
    }
  }

  /**
   * {@code WHERE}.
   *
   * @param filter the condition
   */
  record Where(Filter filter) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("WHERE ").render(filter);
    }
  }

  /**
   * {@code GROUP BY}.
   *
   * @param columns the grouping values
   */
  record GroupBy(List<Expression> columns) implements Clause {

    public GroupBy {
      columns = List.copyOf(columns);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("GROUP BY ").render(columns, ", ");
    }
  }

  /**
   * {@code HAVING}.
   *
   * @param filter the condition on each group
   */
  record Having(Filter filter) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("HAVING ").render(filter);
    }
  }

  /**
   * {@code ORDER BY}.
   *
   * @param sorts the sort keys, in priority order
   */
  record OrderBy(List<Sort> sorts) implements Clause {

    public OrderBy {
      sorts = List.copyOf(sorts);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("ORDER BY ").render(sorts, ", ");
    }
  }

  /**
   * A sort key of {@link OrderBy}.
   *
   * @param value the value to sort by
   * @param direction the sort direction
   */
  record Sort(Expression value, Direction direction) implements Node {

    @Override
    public void render(StatementWriter sql) {
      sql.render(value).append(direction == Direction.DESC ? " DESC" : " ASC");
    }
  }

  /**
   * {@code LIMIT}.
   *
   * @param rows the maximum number of rows
   */
  record Limit(long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("LIMIT " + rows);
    }
  }

  /**
   * {@code OFFSET}.
   *
   * @param rows the number of rows to skip
   */
  record Offset(long rows) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("OFFSET " + rows);
    }
  }

  /**
   * A set operator between two {@link Operand} clauses.
   *
   * @param keyword {@code UNION ALL}, {@code UNION}, {@code INTERSECT} or {@code EXCEPT}
   */
  record SetOperator(String keyword) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append(keyword);
    }
  }

  /**
   * A statement on one side of a {@link SetOperator}.
   *
   * @param statement the statement
   */
  record Operand(Statement statement) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.render(statement);
    }
  }

  /**
   * {@code INSERT INTO} with the statement's table and a column list. An empty list renders no column list, for a row
   * that uses the default value of every column.
   *
   * @param columns the columns that the inserted rows set
   */
  record InsertInto(List<String> columns) implements Clause {

    public InsertInto {
      columns = List.copyOf(columns);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("INSERT INTO " + sql.qualifier());
      if (!columns.isEmpty()) {
        sql.append(" (").names(columns).append(")");
      }
    }
  }

  /**
   * {@code VALUES} with one or more rows.
   *
   * @param rows the values of each row, in the order of the {@link InsertInto} columns
   */
  record Values(List<List<Expression>> rows) implements Clause {

    public Values {
      rows = rows.stream().map(List::copyOf).toList();
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("VALUES ");
      for (int i = 0; i < rows.size(); i++) {
        sql.append(i == 0 ? "(" : ", (").render(rows.get(i), ", ").append(")");
      }
    }
  }

  /** {@code DEFAULT VALUES}: one row that uses the default value of every column. */
  record DefaultValues() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("DEFAULT VALUES");
    }
  }

  /**
   * {@code ON CONFLICT}. When an inserted row has the same key as an existing row, the existing row is updated or
   * kept unchanged.
   *
   * @param key the columns of the primary key or unique index
   * @param updates the columns to copy from the inserted row with {@code EXCLUDED}. An empty list renders
   * {@code DO NOTHING}.
   */
  record OnConflict(List<String> key, List<String> updates) implements Clause {

    public OnConflict {
      key = List.copyOf(key);
      updates = List.copyOf(updates);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("ON CONFLICT (").names(key).append(")");
      if (updates.isEmpty()) {
        sql.append(" DO NOTHING");
        return;
      }
      sql.append(" DO UPDATE SET ");
      for (int i = 0; i < updates.size(); i++) {
        sql.append(i == 0 ? "" : ", ").name(updates.get(i)).append(" = EXCLUDED.").name(updates.get(i));
      }
    }
  }

  /** {@code UPDATE} with the statement's table. */
  record Update() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("UPDATE " + sql.qualifier());
    }
  }

  /**
   * {@code SET}.
   *
   * @param assignments the column assignments
   */
  record Set(List<Assignment> assignments) implements Clause {

    public Set {
      assignments = List.copyOf(assignments);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("SET ").render(assignments, ", ");
    }
  }

  /**
   * A column assignment of {@link Set}: {@code column = value}.
   *
   * @param column the column name
   * @param value the new value
   */
  record Assignment(String column, Expression value) implements Node {

    @Override
    public void render(StatementWriter sql) {
      sql.name(column).append(" = ").render(value);
    }
  }

  /** {@code DELETE FROM} with the statement's table. */
  record DeleteFrom() implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append("DELETE FROM " + sql.qualifier());
    }
  }

  /**
   * {@code RETURNING}: the rows that an INSERT, UPDATE or DELETE wrote.
   *
   * @param columns the columns to return. An empty list returns all columns.
   */
  record Returning(List<String> columns) implements Clause {

    public Returning {
      columns = List.copyOf(columns);
    }

    @Override
    public void render(StatementWriter sql) {
      sql.append("RETURNING ");
      if (columns.isEmpty()) {
        sql.append("*");
      }
      sql.names(columns);
    }
  }

  /**
   * The lock of the rows that a query reads: {@code FOR SHARE}, {@code FOR UPDATE}, {@code FOR UPDATE SKIP LOCKED}
   * or {@code FOR UPDATE NOWAIT}, at the end of the statement. A dialect whose database locks rows another way, or
   * not at all, decorates it into its own syntax.
   *
   * @param mode the lock
   */
  record Lock(LockMode mode) implements Clause {

    @Override
    public void render(StatementWriter sql) {
      sql.append(switch (mode) {
        case SHARE -> "FOR SHARE";
        case UPDATE -> "FOR UPDATE";
        case UPDATE_SKIP_LOCKED -> "FOR UPDATE SKIP LOCKED";
        case UPDATE_NOWAIT -> "FOR UPDATE NOWAIT";
      });
    }
  }
}
