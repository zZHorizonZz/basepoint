package dev.horizon.basepoint.mysql;

import dev.horizon.aquery.ParameterStyle;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.JsonSyntax;
import dev.horizon.basepoint.PagingSyntax;
import dev.horizon.basepoint.ReturningSyntax;
import dev.horizon.basepoint.UpsertSyntax;

/**
 * The SQL of MySQL, from 8.0.31: {@code LIMIT}, {@code ON DUPLICATE KEY UPDATE} with the alias of the new row, and
 * the JSON of includes with {@code JSON_ARRAYAGG} over a window, which keeps their order. MySQL has no
 * {@code RETURNING}. Its placeholders are {@code ?}.
 */
public final class MySqlDialect implements Dialect {

  private final ParameterStyle parameters;
  private final PagingSyntax paging = new MySqlPaging();
  private final ReturningSyntax returning = new MySqlReturning();
  private final UpsertSyntax upserts = new MySqlUpserts();
  private final JsonSyntax json = new MySqlJson();

  public MySqlDialect() {
    this(ParameterStyle.QUESTION_MARK);
  }

  public MySqlDialect(ParameterStyle parameters) {
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return "MySQL";
  }

  @Override
  public String quote(String name) {
    return "`" + name.replace("`", "``") + "`";
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
  public String defaultValues() {
    return " () VALUES ()";
  }
}
