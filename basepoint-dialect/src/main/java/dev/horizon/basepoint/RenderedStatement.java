package dev.horizon.basepoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The SQL text of a statement and the values of its placeholders. Pass both to any database client, such as JDBC,
 * R2DBC or the Vert.x SQL client.
 *
 * @param text the SQL text, with placeholders in the style of the dialect
 * @param params the placeholder values in the order they appear in {@code text}, which can include {@code null}
 */
public record RenderedStatement(String text, List<Object> params) {

  public RenderedStatement {
    params = Collections.unmodifiableList(new ArrayList<>(params));
  }
}
