package dev.horizon.basepoint;

import dev.horizon.aquery.ParameterStyle;

final class StandardDialect implements Dialect {

  private final Decorator decorator;

  StandardDialect() {
    this(Decorator.none());
  }

  StandardDialect(Decorator decorator) {
    this.decorator = decorator;
  }

  @Override
  public String name() {
    return "Standard";
  }

  @Override
  public String quote(String name) {
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  @Override
  public ParameterStyle parameters() {
    return ParameterStyle.QUESTION_MARK;
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }
}
