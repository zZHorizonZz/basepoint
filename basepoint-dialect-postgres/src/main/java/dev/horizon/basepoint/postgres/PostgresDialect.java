package dev.horizon.basepoint.postgres;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.UpsertSyntax;

/**
 * The SQL of PostgreSQL, which writes the standard SQL of basepoint: {@code LIMIT}, {@code RETURNING} and
 * {@code ON CONFLICT}. Its placeholders are {@code $1} by default, as its protocol and Vert.x take them; JDBC takes
 * {@link ParameterStyle#QUESTION_MARK}.
 */
public final class PostgresDialect implements Dialect {

  private final ParameterStyle parameters;
  private final PagingSyntax paging = new PagingSyntax();
  private final ReturningSyntax returning = new ReturningSyntax();
  private final UpsertSyntax upserts = new UpsertSyntax();
  private final JsonSyntax json = new PostgresJson();

  public PostgresDialect() {
    this(ParameterStyle.DOLLAR);
  }

  public PostgresDialect(ParameterStyle parameters) {
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return "PostgreSQL";
  }

  @Override
  public String quote(String name) {
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  @Override
  public ParameterStyle parameters() {
    return parameters;
  }

  @Override
  public PagingSyntax paging() {
    return paging;
  }

  @Override
  public ReturningSyntax returning() {
    return returning;
  }

  @Override
  public UpsertSyntax upserts() {
    return upserts;
  }

  @Override
  public JsonSyntax json() {
    return json;
  }
}
