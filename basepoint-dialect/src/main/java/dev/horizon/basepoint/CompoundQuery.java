package dev.horizon.basepoint;

/**
 * The rows of reads that UNION, INTERSECT or EXCEPT join, from left to right as the calls read. An order and a limit
 * are of the rows of all of them. Each step gives a new compound query.
 */
public interface CompoundQuery extends Pageable<CompoundQuery>, Combinable {

  /**
   * Gives each row of a union once, as UNION does.
   *
   * @return the compound query of distinct rows
   */
  CompoundQuery distinct();
}
