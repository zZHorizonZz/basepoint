package dev.horizon.basepoint;

/**
 * A read whose rows join the rows of another read. The reads give the same columns, which the database checks.
 */
public interface Combinable extends Buildable {

  /**
   * Gives the rows of this read and of another, also the rows that both give; {@link CompoundQuery#distinct()} gives
   * each of them once.
   *
   * @param other the other read
   * @return the rows of both
   */
  CompoundQuery union(Combinable other);

  /**
   * Gives the distinct rows that this read and another both give.
   *
   * @param other the other read
   * @return the rows of both
   */
  CompoundQuery intersect(Combinable other);

  /**
   * Gives the distinct rows of this read that another does not give.
   *
   * @param other the other read
   * @return the rows of this read only
   */
  CompoundQuery except(Combinable other);
}
