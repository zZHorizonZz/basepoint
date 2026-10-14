package dev.horizon.basepoint;

/**
 * A read that you can combine with another read through a set operator. Both reads must return the same columns.
 */
public interface Combinable extends Buildable {

  /**
   * Returns the rows of both reads, including duplicates ({@code UNION ALL}). Call
   * {@link CompoundQuery#distinct()} to remove duplicates.
   *
   * @param other the other read
   * @return the compound query
   */
  CompoundQuery union(Combinable other);

  /**
   * Returns the distinct rows that both reads return ({@code INTERSECT}).
   *
   * @param other the other read
   * @return the compound query
   */
  CompoundQuery intersect(Combinable other);

  /**
   * Returns the distinct rows of this read that the other read does not return ({@code EXCEPT}).
   *
   * @param other the other read
   * @return the compound query
   */
  CompoundQuery except(Combinable other);
}
