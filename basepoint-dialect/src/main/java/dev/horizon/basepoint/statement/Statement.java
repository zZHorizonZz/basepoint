package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.InvalidQueryException;

import dev.horizon.basepoint.filter.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A SQL statement as a list of clauses over a source.
 *
 * <p>
 * SELECT, INSERT, UPDATE and DELETE statements all use this type. The clauses define the kind of statement. For
 * example, a statement with {@link Update} and {@link Assignments} is an UPDATE.
 *
 * <p>
 * Statements are immutable. Each method that changes a statement returns a new statement.
 *
 * @param source the rows that the clauses refer to
 * @param clauses the clauses, in SQL order
 */
public record Statement(Source source, List<Clause> clauses) implements Node {

  public Statement {
    Objects.requireNonNull(source, "source");
    clauses = List.copyOf(clauses);
  }

  @Override
  public void render(StatementWriter sql) {
    source.enter(sql, () -> {
      clauses.forEach(clause -> clause.declare(sql));
      for (int i = 0; i < clauses.size(); i++) {
        sql.append(i == 0 ? "" : " ").render(clauses.get(i));
      }
    });
  }

  /**
   * Returns the first clause of a type.
   *
   * @param type the clause type
   * @param <C> the clause type
   * @return the clause, or an empty optional if the statement has no clause of that type
   */
  public <C extends Clause> Optional<C> find(Class<C> type) {
    return clauses.stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  /**
   * Returns whether the statement has a clause of a type.
   *
   * @param type the clause type
   * @return {@code true} if the statement has a clause of that type
   */
  public boolean has(Class<? extends Clause> type) {
    return find(type).isPresent();
  }

  /**
   * Returns a copy of this statement with a clause added at the end.
   *
   * @param clause the clause to add
   * @return the new statement
   */
  public Statement with(Clause clause) {
    List<Clause> more = new ArrayList<>(clauses);
    more.add(clause);
    return new Statement(source, more);
  }

  /**
   * Returns a copy of this statement with one clause replaced by zero or more clauses.
   *
   * @param clause the clause to replace, which must be in this statement
   * @param with the clauses to insert in its place
   * @return the new statement
   * @throws IllegalArgumentException if this statement does not contain {@code clause}
   */
  public Statement replace(Clause clause, Clause... with) {
    int index = indexOf(clause);
    List<Clause> copy = new ArrayList<>(clauses);
    copy.remove(index);
    copy.addAll(index, List.of(with));
    return new Statement(source, copy);
  }

  /**
   * Returns a copy of this statement without a clause.
   *
   * @param clause the clause to remove, which must be in this statement
   * @return the new statement
   * @throws IllegalArgumentException if this statement does not contain {@code clause}
   */
  public Statement remove(Clause clause) {
    return replace(clause);
  }

  /**
   * Returns a copy of this statement with a clause inserted before the first clause of a type. If no clause has that
   * type, the clause is added at the end.
   *
   * @param anchor the clause type to insert before
   * @param clause the clause to insert
   * @return the new statement
   */
  public Statement before(Class<? extends Clause> anchor, Clause clause) {
    return find(anchor).map(match -> replace(match, clause, match)).orElseGet(() -> with(clause));
  }

  /**
   * Returns a copy of this statement with a clause inserted after the first clause of a type. If no clause has that
   * type, the clause is added at the end.
   *
   * @param anchor the clause type to insert after
   * @param clause the clause to insert
   * @return the new statement
   */
  public Statement after(Class<? extends Clause> anchor, Clause clause) {
    return find(anchor).map(match -> replace(match, match, clause)).orElseGet(() -> with(clause));
  }

  /**
   * Returns a copy of this statement narrowed by a filter. The filter joins the filter of an existing {@code WHERE}
   * clause with {@code AND}, or a new {@code WHERE} clause goes after the {@code FROM}, the {@code SET} or the
   * {@code DELETE FROM} clause.
   *
   * @param filter the filter
   * @return the new statement
   * @throws IllegalStateException if the statement is not a SELECT, an UPDATE or a DELETE
   */
  public Statement where(Filter filter) {
    Optional<Where> where = find(Where.class);
    if (where.isPresent()) {
      return replace(where.get(), new Where(Filter.and(where.get().filter(), filter)));
    }
    if (!has(Select.class) && !has(Assignments.class) && !has(DeleteFrom.class)) {
      throw new IllegalStateException("Only a SELECT, an UPDATE or a DELETE has a filter, but the statement has the"
          + " clauses " + clauses + ".");
    }
    return add(new Where(filter), List.of(With.class, Select.class, Update.class, Assignments.class,
        DeleteFrom.class, From.class, Join.class, Where.class, GroupBy.class, Having.class, OrderBy.class, Limit.class,
        Offset.class, Lock.class, Returning.class));
  }

  /**
   * Checks that the statement can be part of another statement, such as a subquery or the rows of an include.
   *
   * @throws InvalidQueryException if the statement has WITH queries, which SQL Server allows only at the start of a
   * statement
   */
  public void requireNestable() {
    if (has(With.class)) {
      throw new InvalidQueryException("A statement inside another statement cannot have WITH queries, because SQL"
          + " Server allows WITH only at the start of a statement. Call with() on the outermost query.");
    }
  }

  /**
   * Returns all clauses of a type.
   *
   * @param type the clause type
   * @param <C> the clause type
   * @return the clauses, in statement order
   */
  public <C extends Clause> List<C> findAll(Class<C> type) {
    return clauses.stream().filter(type::isInstance).map(type::cast).toList();
  }

  /**
   * Returns a copy of this statement with a clause that replaces the first clause of its type, or with the clause
   * added as {@link #add} adds it.
   *
   * @param clause the clause
   * @param order the clause types in the order that SQL writes them
   * @return the new statement
   */
  public Statement put(Clause clause, List<Class<? extends Clause>> order) {
    return clauses.stream().filter(earlier -> earlier.getClass().equals(clause.getClass())).findFirst()
        .map(earlier -> replace(earlier, clause)).orElseGet(() -> add(clause, order));
  }

  /**
   * Returns a copy of this statement with a clause after the clauses of its type and of the types before it, and
   * before the clauses of the types after it.
   *
   * @param clause the clause
   * @param order the clause types in the order that SQL writes them
   * @return the new statement
   */
  public Statement add(Clause clause, List<Class<? extends Clause>> order) {
    int rank = order.indexOf(clause.getClass());
    int index = 0;
    while (index < clauses.size() && order.indexOf(clauses.get(index).getClass()) <= rank) {
      index++;
    }
    List<Clause> copy = new ArrayList<>(clauses);
    copy.add(index, clause);
    return new Statement(source, copy);
  }

  private int indexOf(Clause clause) {
    for (int i = 0; i < clauses.size(); i++) {
      if (clauses.get(i).equals(clause)) {
        return i;
      }
    }
    throw new IllegalArgumentException("The statement does not contain the clause " + clause + ".");
  }
}
