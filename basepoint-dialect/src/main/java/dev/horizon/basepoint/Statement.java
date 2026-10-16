package dev.horizon.basepoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A SQL statement as a list of clauses over a source.
 *
 * <p>
 * SELECT, INSERT, UPDATE and DELETE statements all use this type. The clauses define the kind of statement. For
 * example, a statement with {@link Clause.Update} and {@link Clause.Set} is an UPDATE.
 *
 * <p>
 * Statements are immutable. Each method that changes a statement returns a new statement.
 *
 * @param source the rows that the clauses refer to, or {@code null} for a statement without rows, such as a UNION
 * @param clauses the clauses, in SQL order
 */
public record Statement(Source source, List<Clause> clauses) implements Node {

  public Statement {
    clauses = List.copyOf(clauses);
  }

  @Override
  public void render(StatementWriter sql) {
    if (source == null) {
      renderClauses(sql);
      return;
    }
    sql.scoped(source.qualifier(sql), source.rows(), () -> renderClauses(sql));
  }

  private void renderClauses(StatementWriter sql) {
    for (int i = 0; i < clauses.size(); i++) {
      sql.append(i == 0 ? "" : " ").render(clauses.get(i));
    }
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
    List<Clause> changed = new ArrayList<>(clauses);
    changed.remove(index);
    changed.addAll(index, List.of(with));
    return new Statement(source, changed);
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
    return find(anchor).map(found -> replace(found, clause, found)).orElseGet(() -> with(clause));
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
    return find(anchor).map(found -> replace(found, found, clause)).orElseGet(() -> with(clause));
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
    Optional<Clause.Where> where = find(Clause.Where.class);
    if (where.isPresent()) {
      return replace(where.get(), new Clause.Where(Filter.and(where.get().filter(), filter)));
    }
    for (Class<? extends Clause> anchor : List.of(Clause.From.class, Clause.Set.class, Clause.DeleteFrom.class)) {
      Optional<? extends Clause> found = find(anchor);
      if (found.isPresent()) {
        return replace(found.get(), found.get(), new Clause.Where(filter));
      }
    }
    throw new IllegalStateException("Only a SELECT, an UPDATE or a DELETE has a filter, but the statement has the"
        + " clauses " + clauses + ".");
  }

  private int indexOf(Clause clause) {
    for (int i = 0; i < clauses.size(); i++) {
      if (clauses.get(i) == clause) {
        return i;
      }
    }
    throw new IllegalArgumentException("The statement does not contain the clause " + clause + ".");
  }
}
