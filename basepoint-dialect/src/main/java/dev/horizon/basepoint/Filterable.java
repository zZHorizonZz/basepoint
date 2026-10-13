package dev.horizon.basepoint;

/**
 * The filters of a builder: of a query, an update or a delete. A row that a filter does not match is left out.
 *
 * @param <S> the type of the builder itself, which each step gives
 */
public interface Filterable<S extends Filterable<S>> {

  /**
   * Adds a filter. Two filters must both match.
   *
   * @param filter the filter
   * @return the builder with the filter
   */
  S where(Filter filter);

  /**
   * Adds a filter of the EBNF grammar of AIP-160, as {@link Filter#of(String)} reads it.
   *
   * @param filter the filter, for example {@code state = "OPEN" AND total > 100}
   * @return the builder with the filter
   */
  S where(String filter);
}
