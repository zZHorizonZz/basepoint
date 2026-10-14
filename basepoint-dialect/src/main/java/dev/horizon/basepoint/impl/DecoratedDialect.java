package dev.horizon.basepoint.impl;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Decorator;
import dev.horizon.basepoint.Dialect;

/**
 * A dialect that applies an application decorator before the decorator of the wrapped dialect.
 */
public final class DecoratedDialect implements Dialect {

  private final Dialect dialect;
  private final Decorator decorator;

  public DecoratedDialect(Dialect dialect, Decorator decorator) {
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
