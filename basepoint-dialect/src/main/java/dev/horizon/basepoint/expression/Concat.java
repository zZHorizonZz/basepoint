package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * Texts joined into one, rendered as the standard {@code (a || b)}. Dialects whose database joins texts otherwise
 * decorate it.
 *
 * @param texts the texts, at least two
 */
public record Concat(List<Expression> texts) implements Expression {

  public Concat {
    texts = List.copyOf(texts);
    if (texts.size() < 2) {
      throw new IllegalArgumentException("concat needs at least two texts, but has " + texts.size() + ".");
    }
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append("(").render(texts, " || ").append(")");
  }
}
