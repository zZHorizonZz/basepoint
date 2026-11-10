package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * A {@code LIKE} match of literal text parts with any text between them. For example, {@code ["ab", ""]} matches
 * strings that start with {@code ab}. The filter escapes {@code %}, {@code _} and {@code [} in the parts with
 * {@code !}.
 *
 * @param operand the string to test
 * @param parts the literal parts
 * @param negated {@code true} to render {@code NOT LIKE}
 */
public record Match(Expression operand, List<String> parts, boolean negated) implements Filter {

  public Match {
    parts = List.copyOf(parts);
  }

  private static final char ESCAPE = '!';
  private static final String WILDCARDS = "%_[";

  @Override
  public void render(StatementWriter sql) {
    StringBuilder pattern = new StringBuilder();
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) {
        pattern.append('%');
      }
      for (char character : parts.get(i).toCharArray()) {
        if (character == ESCAPE || WILDCARDS.indexOf(character) >= 0) {
          pattern.append(ESCAPE);
        }
        pattern.append(character);
      }
    }
    sql.append("(").render(operand).append(negated ? " NOT LIKE " : " LIKE ").param(pattern.toString())
        .append(" ESCAPE '" + ESCAPE + "')");
  }
}
