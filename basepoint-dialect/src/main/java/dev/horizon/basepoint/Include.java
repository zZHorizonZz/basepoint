package dev.horizon.basepoint;

import dev.horizon.basepoint.Clause.OrderBy;
import dev.horizon.basepoint.Clause.Select;
import dev.horizon.basepoint.Clause.Sort;
import dev.horizon.basepoint.Expression.As;
import dev.horizon.basepoint.Expression.Column;
import dev.horizon.basepoint.Expression.Name;
import dev.horizon.basepoint.Expression.RowNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Related rows as a JSON column: an array of objects, or a single object.
 *
 * <p>
 * Standard SQL has no syntax for this. Each dialect decorates {@code Include} into its database's JSON functions. The
 * methods of this record give a dialect what it needs to do that: the related rows, their sort order, a row number
 * for databases whose JSON aggregates have no order, and the column names.
 *
 * @param shape whether the column holds an array or a single object
 * @param rows the statement that selects the related rows
 */
public record Include(Shape shape, Statement rows) implements Expression {

  public static final String ROW_NUMBER = "bp_n";

  /** The JSON shape of an include. */
  public enum Shape {
    /** An array of objects. An empty array if there are no related rows. */
    MANY,
    /** The first related row as an object. {@code null} if there are no related rows. */
    ONE
  }

  @Override
  public void render(StatementWriter sql) {
    throw new UnsupportedOperationException("The " + sql.dialect().name() + " dialect does not render includes.");
  }

  /**
   * Returns whether the related rows have a sort order.
   *
   * @return {@code true} if the statement has an {@code ORDER BY} clause
   */
  public boolean ordered() {
    return rows.has(OrderBy.class);
  }

  /**
   * Returns the related rows with an extra column, {@link #ROW_NUMBER}, that numbers each row in sort order.
   *
   * @return the numbered statement, or the statement unchanged if it has no sort order
   */
  public Statement numbered() {
    if (!ordered()) {
      return rows;
    }
    Select select = select();
    return rows.replace(select, select.with(new As(new RowNumber(sorts()), ROW_NUMBER)));
  }

  /**
   * Returns the sort keys of the related rows. A key that names a column of the SELECT list is replaced by that
   * column's value, because window functions and aggregates cannot refer to the aliases of their own SELECT list.
   *
   * @return the sort keys, or an empty list if the rows have no sort order
   */
  public List<Sort> sorts() {
    if (!ordered()) {
      return List.of();
    }
    Select select = select();
    List<Sort> sorts = new ArrayList<>();
    for (Sort sort : rows.find(OrderBy.class).orElseThrow().sorts()) {
      sorts.add(sort.value() instanceof Name name ? new Sort(valueOf(select, name.name()), sort.direction()) : sort);
    }
    return sorts;
  }

  /**
   * Returns whether the related rows select all columns of their table.
   *
   * @return {@code true} if the query names no columns
   */
  public boolean allColumns() {
    return select().allColumns();
  }

  /**
   * Returns the named columns of the related rows.
   *
   * @return the columns, in SELECT order
   * @throws IllegalStateException if a column has no name
   */
  public List<Output> outputs() {
    List<Output> outputs = new ArrayList<>();
    for (Expression column : select().columns()) {
      outputs.add(switch (column) {
        case Column named -> new Output(named.name(), false);
        case As as -> new Output(as.name(), as.value() instanceof Include);
        default -> throw new IllegalStateException("The include column " + column + " has no name.");
      });
    }
    return outputs;
  }

  /**
   * A named column of the related rows.
   *
   * @param name the column name
   * @param json {@code true} if the column is itself an include and holds JSON
   */
  public record Output(String name, boolean json) {
  }

  /**
   * Renders a JSON object of one related row from its named columns, for example
   * {@code json_object('id', "t1"."id", 'sku', "t1"."sku")}.
   *
   * @param sql the writer that receives the SQL
   * @param function the database function that builds an object from name and value pairs
   * @param alias the alias of the derived table that holds the related rows
   * @param json converts a column that holds JSON text into a JSON value
   * @throws UnsupportedOperationException if the related rows select all columns
   */
  public void object(StatementWriter sql, String function, String alias, UnaryOperator<String> json) {
    if (allColumns()) {
      throw new UnsupportedOperationException(sql.dialect().name() + " can build JSON only from named columns."
          + " List the columns of the included query with columns().");
    }
    List<Output> outputs = outputs();
    sql.append(function + "(");
    for (int i = 0; i < outputs.size(); i++) {
      Output output = outputs.get(i);
      String value = alias + "." + sql.dialect().quote(output.name());
      sql.append(i == 0 ? "" : ", ").append("'" + output.name().replace("'", "''") + "', ")
          .append(output.json() ? json.apply(value) : value);
    }
    sql.append(")");
  }

  private Select select() {
    return rows.find(Select.class).orElseThrow(() -> new IllegalStateException("An include must select rows."));
  }

  private static Expression valueOf(Select select, String name) {
    for (Expression column : select.columns()) {
      if (column instanceof As as && as.name().equals(name)) {
        return as.value();
      }
    }
    return new Name(name);
  }
}
