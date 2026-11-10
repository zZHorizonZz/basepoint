package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * {@code OR} of filters. An empty list renders {@code (1 = 0)}.
 *
 * @param operands the filters
 */
public record Or(List<Filter> operands) implements Filter {

  public Or {
    operands = List.copyOf(operands);
  }

  @Override
  public void render(StatementWriter sql) {
    new Junction(operands, " OR ", "(1 = 0)").render(sql);
  }
}
