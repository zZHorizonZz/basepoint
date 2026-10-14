package dev.horizon.basepoint;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.impl.CollectionImpl;
import dev.horizon.basepoint.impl.DecoratedDialect;

/**
 * The SQL of one database.
 *
 * <p>
 * A dialect defines how to quote identifiers, how to write placeholders, and a {@link Decorator} that rewrites the
 * nodes that the database writes differently from standard SQL. Start each statement from
 * {@link #collection(String)}.
 */
public interface Dialect {

  /**
   * Returns the database name.
   *
   * @return the name, for example {@code PostgreSQL}
   */
  String name();

  /**
   * Quotes an identifier.
   *
   * @param name the identifier, such as a table name, column name or alias
   * @return the quoted identifier
   */
  String quote(String name);

  /**
   * Returns the placeholder style. Choose the style that your database client expects, for example
   * {@link ParameterStyle#QUESTION_MARK} for JDBC.
   *
   * @return the placeholder style
   */
  ParameterStyle parameters();

  /**
   * Returns the decorator that rewrites nodes into the SQL of this database.
   *
   * @return the decorator
   */
  Decorator decorator();

  /**
   * Returns a copy of this dialect that applies an extra decorator before its own.
   *
   * @param decorator the decorator to apply first, for example one that adds a tenant filter
   * @return the new dialect
   */
  default Dialect decorated(Decorator decorator) {
    return new DecoratedDialect(this, decorator);
  }

  /**
   * Returns a reference to a table, which starts queries and writes.
   *
   * @param table the table name, for example {@code orders} or {@code shop.orders}
   * @return the collection reference
   */
  default CollectionReference collection(String table) {
    return new CollectionImpl(this, table);
  }
}
