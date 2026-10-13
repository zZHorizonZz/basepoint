package dev.horizon.basepoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A statement that a builder made: its text, with the placeholders of its dialect, and the values that they take, in
 * order. Any client runs it: JDBC, R2DBC or Vert.x.
 *
 * @param text the SQL
 * @param params the values of the placeholders, in the order of the text; a value can be null
 */
public record Statement(String text, List<Object> params) {

  public Statement {
    params = Collections.unmodifiableList(new ArrayList<>(params));
  }
}
