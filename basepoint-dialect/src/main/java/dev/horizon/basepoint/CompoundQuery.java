package dev.horizon.basepoint;

/**
 * Two or more reads combined with {@code UNION}, {@code INTERSECT} or {@code EXCEPT}.
 *
 * <p>
 * The operators apply from left to right, in the order of the calls. Sorting, limits and offsets apply to the
 * combined rows. Compound queries are immutable. Each method returns a new compound query.
 */
public interface CompoundQuery extends Pageable<CompoundQuery>, Combinable {

  /**
   * Removes duplicate rows from a union ({@code UNION} instead of {@code UNION ALL}).
   *
   * @return a new compound query
   */
  CompoundQuery distinct();
}
