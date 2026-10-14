package dev.horizon.basepoint;

/**
 * A part of a statement tree. Statements, clauses, expressions and filters are all nodes.
 *
 * <p>
 * Each node renders itself as standard SQL. A node renders its child nodes through {@link StatementWriter#render}, so
 * the dialect's {@link Decorator} can rewrite every child before it renders.
 */
@FunctionalInterface
public interface Node {

  /**
   * Renders this node.
   *
   * @param sql the writer that receives the SQL
   */
  void render(StatementWriter sql);
}
