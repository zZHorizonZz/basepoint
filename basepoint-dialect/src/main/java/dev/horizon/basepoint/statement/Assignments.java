package dev.horizon.basepoint.statement;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SET}.
 *
 * @param assignments the column assignments
 */
public record Assignments(List<Assignment> assignments) implements Clause {

  public Assignments {
    assignments = List.copyOf(assignments);
  }

  /**
   * Returns a copy of this clause with an assignment, which replaces an earlier assignment of its column.
   *
   * @param assignment the assignment
   * @return the new clause
   */
  public Assignments put(Assignment assignment) {
    List<Assignment> all = new ArrayList<>(assignments);
    all.removeIf(earlier -> earlier.column().equals(assignment.column()));
    all.add(assignment);
    return new Assignments(all);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("SET ").render(assignments, ", ");
  }
}
