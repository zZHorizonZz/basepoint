package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.Clause;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Include;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.StatementWriter;
import java.util.List;

/**
 * An include rendered as {@code JSON_ARRAYAGG} of {@code JSON_OBJECT}.
 *
 * <p>
 * MariaDB does not allow references to an enclosing query inside a derived table. So the aggregate runs in the
 * correlated subquery itself: the SELECT list becomes {@code JSON_ARRAYAGG(JSON_OBJECT(...) ORDER BY ... LIMIT ...)},
 * and the subquery's own {@code ORDER BY}, {@code LIMIT} and {@code OFFSET} move into the aggregate.
 *
 * @param include the include
 */
record MariaDbJson(Include include) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    Statement rows = include.rows();
    Clause.Select select = rows.find(Clause.Select.class).orElseThrow();
    JsonObject object = new JsonObject(include);
    if (include.shape() == Include.Shape.ONE) {
      sql.append("(").render(rows.replace(select, new Clause.Select(false, List.of(object)))).append(")");
      return;
    }
    Clause.Limit limit = rows.find(Clause.Limit.class).orElse(null);
    Clause.Offset offset = rows.find(Clause.Offset.class).orElse(null);
    Statement aggregated = rows.replace(select, new Clause.Select(false,
        List.of(new ArrayAgg(object, include.sorts(), limit, offset))));
    for (Class<? extends Clause> moved : List.of(Clause.OrderBy.class, Clause.Limit.class, Clause.Offset.class)) {
      Clause clause = aggregated.find(moved).orElse(null);
      if (clause != null) {
        aggregated = aggregated.remove(clause);
      }
    }
    sql.append("COALESCE((").render(aggregated).append("), JSON_ARRAY())");
  }

  /**
   * {@code JSON_OBJECT} of the named columns of the related rows.
   *
   * @param include the include
   */
  record JsonObject(Include include) implements Expression {

    @Override
    public void render(StatementWriter sql) {
      if (include.allColumns()) {
        throw new UnsupportedOperationException(MariaDbDialect.NAME + " can build JSON only from named columns."
            + " List the columns of the included query with columns().");
      }
      List<Expression> columns = include.rows().find(Clause.Select.class).orElseThrow().columns();
      sql.append("JSON_OBJECT(");
      for (int i = 0; i < columns.size(); i++) {
        Expression column = columns.get(i);
        String name = switch (column) {
          case Expression.Column named -> named.name();
          case Expression.As as -> as.name();
          default -> throw new IllegalStateException("The include column " + column + " has no name.");
        };
        Expression value = column instanceof Expression.As as ? as.value() : column;
        sql.append(i == 0 ? "" : ", ").append("'" + name.replace("'", "''") + "', ").render(value);
      }
      sql.append(")");
    }
  }

  /**
   * {@code JSON_ARRAYAGG(value ORDER BY ... LIMIT ... OFFSET ...)}.
   *
   * @param value the value of each row
   * @param sorts the sort keys, which can be empty
   * @param limit the maximum number of rows, or {@code null} for no limit
   * @param offset the number of rows to skip, or {@code null} for none
   */
  record ArrayAgg(Expression value, List<Clause.Sort> sorts, Clause.Limit limit, Clause.Offset offset)
      implements Expression {

    @Override
    public void render(StatementWriter sql) {
      sql.append("JSON_ARRAYAGG(").render(value);
      if (!sorts.isEmpty()) {
        sql.append(" ORDER BY ").render(sorts, ", ");
      }
      if (limit != null || offset != null) {
        sql.append(" LIMIT " + (limit == null ? MariaDbLimits.ALL_ROWS : Long.toString(limit.rows())));
      }
      if (offset != null) {
        sql.append(" OFFSET " + offset.rows());
      }
      sql.append(")");
    }
  }
}
