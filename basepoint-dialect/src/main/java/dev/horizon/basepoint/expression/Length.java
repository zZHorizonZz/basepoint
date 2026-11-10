package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;

/**
 * The number of characters of a text, rendered as the standard {@code CHAR_LENGTH(text)}. Dialects whose database
 * counts otherwise decorate it.
 *
 * @param text the text
 */
public record Length(Expression text) implements Expression {

  @Override
  public void render(StatementWriter sql) {
    sql.append("CHAR_LENGTH(").render(text).append(")");
  }
}
