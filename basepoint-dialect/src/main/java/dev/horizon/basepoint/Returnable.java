package dev.horizon.basepoint;

/**
 * A write that can return the rows it wrote. PostgreSQL, MariaDB and SQLite render {@code RETURNING}. SQL Server
 * renders {@code OUTPUT}. MariaDB supports {@code UPDATE ... RETURNING} from version 13.0, and earlier versions reject
 * the statement.
 *
 * @param <S> the builder type that each method returns
 */
public interface Returnable<S extends Returnable<S>> {

  /**
   * Returns the written rows. Inserts and updates return rows as they are after the write. Deletes return rows as
   * they were before the delete.
   *
   * @param columns the columns to return. Pass none to return all columns.
   * @return a new builder that returns the rows
   */
  S returning(String... columns);
}
