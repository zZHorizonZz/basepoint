package dev.horizon.basepoint;

/**
 * A write that gives the rows that it wrote: {@code RETURNING}, or {@code OUTPUT} on SQL Server. MySQL has no form of
 * it, and refuses it.
 *
 * @param <S> the type of the builder itself, which each step gives
 */
public interface Returnable<S extends Returnable<S>> {

  /**
   * Gives the rows that the write wrote: as they are after an insert or an update, and as they were before a delete.
   *
   * @param columns the names of the columns; none for all
   * @return the builder that gives the rows
   */
  S returning(String... columns);
}
