package dev.horizon.basepoint.expression;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * A function that every database writes the same way, such as {@code LOWER(text)}.
 *
 * @param function the function
 * @param arguments the arguments
 */
public record Call(Function function, List<Expression> arguments) implements Expression {

  public Call {
    arguments = List.copyOf(arguments);
  }

  @Override
  public void render(StatementWriter sql) {
    sql.append(function.name() + "(").render(arguments, ", ").append(")");
  }

  /** A function of {@link Call}, named as SQL names it. */
  public enum Function {
    /** A text in lower case. */
    LOWER,
    /** A text in upper case. */
    UPPER,
    /** A text without spaces at its start and end. */
    TRIM,
    /** The absolute value of a number. */
    ABS,
    /** The first value that is not {@code NULL}. */
    COALESCE
  }
}
