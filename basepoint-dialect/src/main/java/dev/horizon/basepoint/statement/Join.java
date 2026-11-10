package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.filter.Filter;

/**
 * The join of a relation: {@code LEFT JOIN table AS alias ON condition}, or {@code JOIN} for an inner join. The
 * condition reads the columns of the table, and the row of the statement as the enclosing row.
 *
 * @param name the name of the relation, which its paths read
 * @param table the table of the relation
 * @param condition the condition of the join
 * @param inner true for an inner join, which keeps only the rows that have a related row
 */
public record Join(String name, String table, Filter condition, boolean inner) implements Clause {

  /**
   * Returns a copy of this join that keeps only the rows that have a related row.
   *
   * @return the inner join
   */
  public Join toInner() {
    return new Join(name, table, condition, true);
  }

  @Override
  public void declare(StatementWriter sql) {
    sql.relate(name);
  }

  @Override
  public void render(StatementWriter sql) {
    String alias = sql.relation(name).orElseThrow(() -> new IllegalStateException("The join of the relation " + name
        + " renders outside the statement that names it."));
    sql.append(inner ? "JOIN " : "LEFT JOIN ").table(table).append(" AS " + alias + " ON ");
    sql.scope(alias, new Source.Table(table), () -> sql.render(condition));
  }
}
