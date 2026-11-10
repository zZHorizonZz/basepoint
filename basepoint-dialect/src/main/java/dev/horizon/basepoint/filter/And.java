package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;

/**
 * {@code AND} of filters. An empty list renders {@code (1 = 1)}.
 *
 * @param operands the filters
 */
public record And(List<Filter> operands) implements Filter {

  public And {
    operands = List.copyOf(operands);
  }

  @Override
  public void render(StatementWriter sql) {
    new Junction(operands, " AND ", "(1 = 1)").render(sql);
  }
}
