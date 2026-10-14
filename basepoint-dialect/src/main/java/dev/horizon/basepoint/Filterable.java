package dev.horizon.basepoint;

/**
 * A builder that accepts filters. Each call adds a filter. A row must match all filters.
 *
 * @param <S> the builder type that each method returns
 */
public interface Filterable<S extends Filterable<S>> {

  /**
   * Adds a filter.
   *
   * @param filter the filter
   * @return a new builder with the filter
   */
  S where(Filter filter);

  /**
   * Adds a filter written in the AIP-160 filter syntax. See {@link Filter#of(String)}.
   *
   * @param filter the filter text, for example {@code state = "OPEN" AND total > 100}
   * @return a new builder with the filter
   */
  S where(String filter);
}
