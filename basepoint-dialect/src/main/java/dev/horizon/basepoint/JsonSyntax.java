package dev.horizon.basepoint;

import java.util.List;

/**
 * The JSON of the related rows of an include: an array of objects, or one object. A database writes JSON in its own
 * way, thus each dialect writes it; this class writes the number of rows, and the objects of the dialects that build
 * them column by column.
 */
public abstract class JsonSyntax {

  /** The column that numbers the related rows in their order, for the dialects that aggregate them. */
  public static final String ROW_NUMBER = "bp_n";

  /** What the column of an include has. */
  public enum Shape {
    /** The rows, as a JSON array of objects. */
    MANY,
    /** The first row, as a JSON object, or null. */
    ONE,
    /** The number of rows. */
    COUNT
  }

  /**
   * The related rows that a read gives as a column of each of its rows.
   *
   * @param name the name of the column
   * @param shape what the column has
   * @param rows the related rows
   */
  public record Include(String name, Shape shape, Rows rows) {
  }

  /** The related rows of an include, as a dialect writes them. */
  public interface Rows {

    /**
     * Tells if the rows have every column of their table, which no list names.
     *
     * @return true for every column
     */
    boolean allColumns();

    /**
     * Gives the columns that the rows have by name: those that the read names, its values and its includes.
     *
     * @return the columns, in order
     */
    List<Output> outputs();

    /**
     * Tells if the rows have an order.
     *
     * @return true if they do
     */
    boolean ordered();

    /**
     * Writes the SELECT of the rows.
     *
     * @param sql the writer of the statement
     * @param numbered true to give each row its number in the order, as the column {@link #ROW_NUMBER}
     * @param json true if the database writes the rows as JSON, which needs each include of the rows as JSON too
     */
    void write(StatementWriter sql, boolean numbered, boolean json);

    /**
     * Writes the SELECT of the number of the rows.
     *
     * @param sql the writer of the statement
     */
    void writeCount(StatementWriter sql);
  }

  /**
   * A column of the related rows.
   *
   * @param name the name of the column
   * @param json true if it is JSON: the rows of an include
   */
  public record Output(String name, boolean json) {
  }

  /**
   * Writes an include as a column of a SELECT: its value, then {@code AS} its name.
   *
   * @param sql the writer of the statement
   * @param include the include
   * @param json true if the database writes the rows of the SELECT as JSON, which needs the include as JSON too
   */
  public void include(StatementWriter sql, Include include, boolean json) {
    switch (include.shape()) {
      case MANY -> many(sql, include.rows());
      case ONE -> one(sql, include.rows());
      case COUNT -> {
        sql.append("(");
        include.rows().writeCount(sql);
        sql.append(")");
      }
    }
    sql.append(" AS ").name(include.name());
  }

  /**
   * Writes the rows as a JSON array of objects, in their order; an empty array for no rows.
   *
   * @param sql the writer of the statement
   * @param rows the rows
   */
  protected abstract void many(StatementWriter sql, Rows rows);

  /**
   * Writes the first row as a JSON object, or null for no rows. The rows are limited to one.
   *
   * @param sql the writer of the statement
   * @param rows the rows
   */
  protected abstract void one(StatementWriter sql, Rows rows);

  /**
   * Writes the object of a row column by column: {@code function('name', alias.name, ...)}, with each include of the
   * row as JSON.
   *
   * @param sql the writer of the statement
   * @param function the function that builds an object of names and values
   * @param alias the alias of the table of the rows
   * @param rows the rows, which name their columns
   * @throws UnsupportedOperationException if the rows have every column, which no list names
   */
  protected void object(StatementWriter sql, String function, String alias, Rows rows) {
    if (rows.allColumns()) {
      throw new UnsupportedOperationException(sql.dialect().name() + " makes JSON of the columns that an include"
          + " names, and has no JSON of a whole row: name them with columns(...)");
    }
    List<Output> outputs = rows.outputs();
    sql.append(function).append("(");
    for (int i = 0; i < outputs.size(); i++) {
      Output output = outputs.get(i);
      String value = alias + "." + sql.dialect().quote(output.name());
      sql.append(i == 0 ? "" : ", ").append("'" + output.name().replace("'", "''") + "', ")
          .append(output.json() ? json(value) : value);
    }
    sql.append(")");
  }

  /**
   * Gives a value that is JSON text as JSON, so that an object has it as JSON, not as a string.
   *
   * @param value the SQL of the value
   * @return the SQL of the JSON: the value itself, if the database keeps the type of JSON
   */
  protected String json(String value) {
    return value;
  }
}
