package dev.horizon.basepoint.sqlserver;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.UpsertSyntax;

/**
 * The SQL of SQL Server: {@code TOP} and {@code OFFSET ... FETCH}, {@code OUTPUT}, {@code MERGE} for upserts, and
 * {@code FOR JSON} for includes. Its placeholders are {@code @p1} by default, as Vert.x takes them; JDBC takes
 * {@link ParameterStyle#QUESTION_MARK}.
 */
public final class SqlServerDialect implements Dialect {

  private final ParameterStyle parameters;
  private final PagingSyntax paging = new SqlServerPaging();
  private final ReturningSyntax returning = new SqlServerReturning();
  private final UpsertSyntax upserts = new SqlServerUpserts();
  private final JsonSyntax json = new SqlServerJson();

  public SqlServerDialect() {
    this(ParameterStyle.AT_P);
  }

  public SqlServerDialect(ParameterStyle parameters) {
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return "SQL Server";
  }

  @Override
  public String quote(String name) {
    return "[" + name.replace("]", "]]") + "]";
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

  @Override
  public String likeWildcards() {
    return "%_[";
  }
}
