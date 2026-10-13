package dev.horizon.basepoint;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.impl.CollectionImpl;

/**
 * The SQL of one database, and its collections.
 *
 * <p>
 * Every database writes the statements of basepoint alike but for a few parts, which a dialect gives: its quotes and
 * placeholders, the limit of rows ({@link PagingSyntax}), the rows that a write gives ({@link ReturningSyntax}), the
 * upsert ({@link UpsertSyntax}) and the JSON of includes ({@link JsonSyntax}). Each part writes standard SQL, and a
 * dialect gives its own where its database writes it in another way.
 */
public interface Dialect {

  /**
   * Gives the name of the database.
   *
   * @return the name, for example {@code PostgreSQL}
   */
  String name();

  /**
   * Quotes a name.
   *
   * @param name the name of a table, a column or an alias
   * @return the name in the quotes of the database
   */
  String quote(String name);

  /**
   * Gives the placeholders of the values, which the client that runs the statements reads: {@code ?} for JDBC.
   *
   * @return the style of the placeholders
   */
  ParameterStyle parameters();

  /**
   * Gives the limit of rows.
   *
   * @return the syntax
   */
  PagingSyntax paging();

  /**
   * Gives the rows that an INSERT, an UPDATE or a DELETE gives.
   *
   * @return the syntax
   */
  ReturningSyntax returning();

  /**
   * Gives the upsert.
   *
   * @return the syntax
   */
  UpsertSyntax upserts();

  /**
   * Gives the JSON of the rows of includes.
   *
   * @return the syntax
   */
  JsonSyntax json();

  /**
   * Gives the characters that are wildcards of LIKE, which a filter of a text escapes.
   *
   * @return the characters
   */
  default String likeWildcards() {
    return "%_";
  }

  /**
   * Gives the end of an INSERT of a row that takes the default of every column.
   *
   * @return the SQL, after the table and the columns that the statement gives
   */
  default String defaultValues() {
    return " DEFAULT VALUES";
  }

  /**
   * Names the rows of a table.
   *
   * @param table the name of the table, whose parts, such as its schema, are separated by dots
   * @return the collection
   */
  default CollectionReference collection(String table) {
    return new CollectionImpl(this, table);
  }
}
