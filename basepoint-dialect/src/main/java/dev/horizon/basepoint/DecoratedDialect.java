package dev.horizon.basepoint;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.statement.Decorator;

final class DecoratedDialect implements Dialect {

  private final Dialect dialect;
  private final Decorator decorator;

  DecoratedDialect(Dialect dialect, Decorator decorator) {
    this.dialect = dialect;
    this.decorator = decorator.then(dialect.decorator());
  }

  @Override
  public String name() {
    return dialect.name();
  }

  @Override
  public String quote(String name) {
    return dialect.quote(name);
  }

  @Override
  public ParameterStyle parameters() {
    return dialect.parameters();
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }
}
