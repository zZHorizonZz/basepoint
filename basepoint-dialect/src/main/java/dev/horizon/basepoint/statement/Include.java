package dev.horizon.basepoint.statement;

import dev.horizon.basepoint.InvalidQueryException;
import dev.horizon.basepoint.UnsupportedFeatureException;
import dev.horizon.basepoint.expression.As;
import dev.horizon.basepoint.expression.Column;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Reference;
import dev.horizon.basepoint.expression.RowNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Related rows as a JSON column: an array of objects, {@link Many}, or a single object, {@link One}.
 *
 * <p>
 * Standard SQL has no syntax for this. Each dialect decorates both records into its database's JSON functions. The
 * methods of this interface give a dialect what it needs to do that: the related rows, their sort order, a row number
 * for databases whose JSON aggregates have no order, and the columns.
 */
public sealed interface Include extends Expression {

  String ROW_NUMBER = "bp_n";

  /**
   * Returns the statement that selects the related rows.
   *
   * @return the statement
   */
  Statement rows();

  @Override
  default void render(StatementWriter sql) {
    throw new UnsupportedFeatureException("The " + sql.dialect().name() + " dialect does not render includes.");
  }

  /**
   * Returns the columns of the related rows.
   *
   * @return the columns, in SELECT order
   * @throws UnsupportedFeatureException if the related rows select all columns, which a JSON object cannot list
   */
  default List<Output> outputs() {
    Select select = rows().find(Select.class).orElseThrow(() -> new IllegalStateException("An include must select"
        + " rows."));
    if (select.allColumns()) {
      throw new UnsupportedFeatureException("A JSON object of related rows needs their column names. List the"
          + " columns of the included query with columns().");
    }
    List<Output> outputs = new ArrayList<>();
    for (Expression value : select.columns()) {
      outputs.add(switch (value) {
        case Column column -> new Output(column.name(), column);
        case As as -> new Output(as.name(), as.value());
        default -> throw new IllegalStateException("The include column " + value + " has no name.");
      });
    }
    return outputs;
  }

  /**
   * Renders a JSON object of one related row from its named columns, for example
   * {@code json_object('id', "t1"."id", 'sku', "t1"."sku")}.
   *
   * @param sql the writer that receives the SQL
   * @param function the database function that builds an object from name and value pairs
   * @param alias the alias of the derived table that holds the related rows
   * @param json converts a column that holds JSON text into a JSON value
   * @throws UnsupportedFeatureException if the related rows select all columns
   */
  default void object(StatementWriter sql, String function, String alias, UnaryOperator<String> json) {
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

  /**
   * A column of the related rows.
   *
   * @param name the column name
   * @param value the value of the column
   */
  record Output(String name, Expression value) {

    /**
     * Returns whether the column is itself an include, which holds JSON.
     *
     * @return {@code true} if it is
     */
    public boolean json() {
      return value instanceof Include;
    }
  }

  /**
   * The first related row as a JSON object, or {@code null} if there is none.
   *
   * @param rows the statement that selects the related row
   */
  record One(Statement rows) implements Include {

    public One {
      rows.requireNestable();
    }
  }

  /**
   * The related rows as a JSON array of objects, or an empty array if there are none.
   *
   * @param rows the statement that selects the related rows
   */
  record Many(Statement rows) implements Include {

    public Many {
      rows.requireNestable();
      if (rows.find(Select.class).filter(select -> select.names(ROW_NUMBER)).isPresent()) {
        throw new InvalidQueryException("An included query cannot select a column named " + ROW_NUMBER + ", which"
            + " includes use to keep the order of their rows. Select it under another name.");
      }
    }

    /**
     * Returns whether the related rows have a sort order.
     *
     * @return {@code true} if the statement has an {@code ORDER BY} clause
     */
    public boolean hasOrder() {
      return rows.has(OrderBy.class);
    }

    /**
     * Returns the related rows with an extra column, {@link #ROW_NUMBER}, that numbers each row in sort order.
     *
     * @return the numbered statement, or the statement unchanged if it has no sort order
     */
    public Statement numberRows() {
      if (!hasOrder()) {
        return rows;
      }
      Select select = rows.find(Select.class).orElseThrow();
      return rows.replace(select, select.with(new As(new RowNumber(sorts()), ROW_NUMBER)));
    }

    /**
     * Returns the sort keys of the related rows. A key that names a column of the SELECT list is replaced by that
     * column's value, because window functions and aggregates cannot refer to the aliases of their own SELECT list.
     *
     * @return the sort keys, or an empty list if the rows have no sort order
     */
    public List<Sort> sorts() {
      if (!hasOrder()) {
        return List.of();
      }
      Select select = rows.find(Select.class).orElseThrow();
      List<Sort> sorts = new ArrayList<>();
      for (Sort sort : rows.find(OrderBy.class).orElseThrow().sorts()) {
        sorts.add(sort.value() instanceof Reference reference ? new Sort(valueOf(select, reference.name()),
            sort.direction()) : sort);
      }
      return sorts;
    }

    private Expression valueOf(Select select, String name) {
      for (Expression column : select.columns()) {
        if (column instanceof As as && as.name().equals(name)) {
          return as.value();
        }
      }
      return new Column(name);
    }
  }
}
