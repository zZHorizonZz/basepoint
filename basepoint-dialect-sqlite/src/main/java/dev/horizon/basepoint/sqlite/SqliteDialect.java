package dev.horizon.basepoint.sqlite;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.UpsertSyntax;

/**
 * The SQL of SQLite, which writes {@code RETURNING} and {@code ON CONFLICT} as PostgreSQL does, and the JSON of
 * includes with {@code json_group_array}, from SQLite 3.44. Its placeholders are {@code ?}.
 */
public final class SqliteDialect implements Dialect {

  private final ParameterStyle parameters;
  private final PagingSyntax paging = new SqlitePaging();
  private final ReturningSyntax returning = new ReturningSyntax();
  private final UpsertSyntax upserts = new UpsertSyntax();
  private final JsonSyntax json = new SqliteJson();

  public SqliteDialect() {
    this(ParameterStyle.QUESTION_MARK);
  }

  public SqliteDialect(ParameterStyle parameters) {
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return "SQLite";
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
