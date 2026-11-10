package dev.horizon.basepoint.filter;

import dev.horizon.aquery.aip160.FilterParser;
import dev.horizon.aquery.ebnf.EbnfFilterParser;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Null;
import dev.horizon.basepoint.statement.Node;
import dev.horizon.basepoint.statement.StatementWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A condition for {@code WHERE} or {@code HAVING}.
 *
 * <p>
 * Build filters with the static methods of this interface, or parse them from text with {@link #of(String)}. Both
 * kinds combine freely. Every value is bound as a placeholder. Strings compare by the collation of the column.
 *
 * <p>
 * To add your own filter, implement this interface and render it with {@link StatementWriter}.
 */
@FunctionalInterface
public interface Filter extends Node {

  /**
   * Parses a filter written in the AIP-160 filter syntax, for example {@code state = "OPEN" AND total > 100}.
   *
   * @param text the filter text
   * @return the filter
   * @throws dev.horizon.aquery.InvalidQueryException if the text is not valid
   * @see #of(String, FilterParser)
   */
  static Text of(String text) {
    return of(text, new EbnfFilterParser());
  }

  /**
   * Parses a filter with an aquery parser, for example {@code new CelFilterParser()} for CEL.
   *
   * <p>
   * The text is parsed here, and translated into filters where it renders. Each field is a column name, the name of a
   * relation of the statement and its column, such as {@code customer.region}, or a column name and a key of the JSON
   * object in the column, such as {@code labels.env}, whose values compare as text. Unquoted values become numbers, booleans or {@code null} when
   * they have that form, and strings otherwise. Quoted values are strings, and {@code *} inside them matches any text.
   * {@code column:*} matches rows where the column is not {@code NULL}, and {@code column:key} the rows whose object
   * has the key.
   *
   * @param text the filter text
   * @param parser the parser
   * @return the filter
   * @throws dev.horizon.aquery.InvalidQueryException if the text is not valid. A field, operator or value without a
   * translation throws {@link dev.horizon.aquery.aip160.InvalidFilterException} when the statement renders.
   */
  static Text of(String text, FilterParser parser) {
    return new Text(parser.parse(text));
  }

  /**
   * Matches rows where a column equals a value. A {@code null} value renders {@code IS NULL}.
   *
   * @param column the column name
   * @param value the value, an {@link Expression}, or {@code null}
   * @return the filter
   */
  static Filter eq(String column, Object value) {
    return eq(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression equals a value. A {@code null} value renders {@code IS NULL}.
   *
   * @param left the expression
   * @param value the value, an {@link Expression}, or {@code null}
   * @return the filter
   */
  static Filter eq(Expression left, Object value) {
    Expression right = Expression.value(value);
    return right instanceof Null ? new IsNull(left, false) : new Compare(left, Compare.Operator.EQ, right);
  }

  /**
   * Matches rows where a column does not equal a value. A {@code null} value renders {@code IS NOT NULL}. Rows where
   * the column is {@code NULL} never match a non-null value.
   *
   * @param column the column name
   * @param value the value, an {@link Expression}, or {@code null}
   * @return the filter
   */
  static Filter ne(String column, Object value) {
    return ne(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression does not equal a value. A {@code null} value renders {@code IS NOT NULL}.
   *
   * @param left the expression
   * @param value the value, an {@link Expression}, or {@code null}
   * @return the filter
   */
  static Filter ne(Expression left, Object value) {
    Expression right = Expression.value(value);
    return right instanceof Null ? new IsNull(left, true) : new Compare(left, Compare.Operator.NE, right);
  }

  /**
   * Matches rows where a column is greater than a value.
   *
   * @param column the column name
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gt(String column, Object value) {
    return gt(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression is greater than a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gt(Expression left, Object value) {
    return new Compare(left, Compare.Operator.GT, Expression.value(value));
  }

  /**
   * Matches rows where a column is greater than or equal to a value.
   *
   * @param column the column name
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gte(String column, Object value) {
    return gte(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression is greater than or equal to a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gte(Expression left, Object value) {
    return new Compare(left, Compare.Operator.GTE, Expression.value(value));
  }

  /**
   * Matches rows where a column is less than a value.
   *
   * @param column the column name
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lt(String column, Object value) {
    return lt(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression is less than a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lt(Expression left, Object value) {
    return new Compare(left, Compare.Operator.LT, Expression.value(value));
  }

  /**
   * Matches rows where a column is less than or equal to a value.
   *
   * @param column the column name
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lte(String column, Object value) {
    return lte(Expression.column(column), value);
  }

  /**
   * Matches rows where an expression is less than or equal to a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lte(Expression left, Object value) {
    return new Compare(left, Compare.Operator.LTE, Expression.value(value));
  }

  /**
   * Matches rows where a column equals one of the values. An empty collection matches no rows. A {@code null}
   * value matches the rows where the column is {@code NULL}, as {@link #eq(String, Object)} does.
   *
   * @param column the column name
   * @param values the values
   * @return the filter
   */
  static Filter in(String column, Collection<?> values) {
    return in(Expression.column(column), values);
  }

  /**
   * Matches rows where an expression equals one of the values. An empty collection matches no rows.
   *
   * @param left the expression
   * @param values the values
   * @return the filter
   */
  static Filter in(Expression left, Collection<?> values) {
    return new In(left, new ArrayList<>(values), false);
  }

  /**
   * Matches rows where a column equals none of the values. An empty collection matches every row. Rows where the
   * column is {@code NULL} match only an empty collection. A {@code null} value adds nothing: those rows already do
   * not match a collection with a value in it, and a collection of only {@code null} matches the rows with a value.
   *
   * @param column the column name
   * @param values the values
   * @return the filter
   */
  static Filter nin(String column, Collection<?> values) {
    return nin(Expression.column(column), values);
  }

  /**
   * Matches rows where an expression equals none of the values. An empty collection matches every row.
   *
   * @param left the expression
   * @param values the values
   * @return the filter
   */
  static Filter nin(Expression left, Collection<?> values) {
    return new In(left, new ArrayList<>(values), true);
  }

  /**
   * Matches rows where a column is between two values, inclusive.
   *
   * @param column the column name
   * @param low the lower bound
   * @param high the upper bound
   * @return the filter
   */
  static Filter between(String column, Object low, Object high) {
    return between(Expression.column(column), low, high);
  }

  /**
   * Matches rows where an expression is between two values, inclusive.
   *
   * @param left the expression
   * @param low the lower bound
   * @param high the upper bound
   * @return the filter
   */
  static Filter between(Expression left, Object low, Object high) {
    return new Between(left, Expression.value(low), Expression.value(high));
  }

  /**
   * Matches rows where a column is {@code NULL}.
   *
   * @param column the column name
   * @return the filter
   */
  static Filter isNull(String column) {
    return isNull(Expression.column(column));
  }

  /**
   * Matches rows where a value is {@code NULL}.
   *
   * @param value the value
   * @return the filter
   */
  static Filter isNull(Expression value) {
    return new IsNull(value, false);
  }

  /**
   * Matches rows where a column is not {@code NULL}.
   *
   * @param column the column name
   * @return the filter
   */
  static Filter isNotNull(String column) {
    return isNotNull(Expression.column(column));
  }

  /**
   * Matches rows where a value is not {@code NULL}.
   *
   * @param value the value
   * @return the filter
   */
  static Filter isNotNull(Expression value) {
    return new IsNull(value, true);
  }

  /**
   * Matches rows where a column starts with a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param prefix the text
   * @return the filter
   */
  static Filter startsWith(String column, String prefix) {
    return startsWith(Expression.column(column), prefix);
  }

  /**
   * Matches rows where a text value starts with a text. Wildcard characters in the text match only themselves.
   *
   * @param value the text value
   * @param prefix the text
   * @return the filter
   */
  static Filter startsWith(Expression value, String prefix) {
    return new Match(value, List.of(prefix, ""), false);
  }

  /**
   * Matches rows where a column ends with a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param suffix the text
   * @return the filter
   */
  static Filter endsWith(String column, String suffix) {
    return endsWith(Expression.column(column), suffix);
  }

  /**
   * Matches rows where a text value ends with a text. Wildcard characters in the text match only themselves.
   *
   * @param value the text value
   * @param suffix the text
   * @return the filter
   */
  static Filter endsWith(Expression value, String suffix) {
    return new Match(value, List.of("", suffix), false);
  }

  /**
   * Matches rows where a column contains a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param part the text
   * @return the filter
   */
  static Filter contains(String column, String part) {
    return contains(Expression.column(column), part);
  }

  /**
   * Matches rows where a text value contains a text. Wildcard characters in the text match only themselves.
   *
   * @param value the text value
   * @param part the text
   * @return the filter
   */
  static Filter contains(Expression value, String part) {
    return new Match(value, List.of("", part, ""), false);
  }

  /**
   * Matches rows where a column matches a {@code LIKE} pattern. The pattern uses the wildcards of the database.
   *
   * @param column the column name
   * @param pattern the pattern, for example {@code M%}
   * @return the filter
   */
  static Filter like(String column, String pattern) {
    return like(Expression.column(column), pattern);
  }

  /**
   * Matches rows where a text value matches a {@code LIKE} pattern. The pattern uses the wildcards of the database.
   *
   * @param value the text value
   * @param pattern the pattern, for example {@code M%}
   * @return the filter
   */
  static Filter like(Expression value, String pattern) {
    return new Like(value, pattern);
  }

  /**
   * Matches rows that match all filters. With no filters, it matches every row.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter and(Filter... filters) {
    return and(List.of(filters));
  }

  /**
   * Matches rows that match all filters. With no filters, it matches every row, and with one, it is that filter.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter and(List<Filter> filters) {
    List<Filter> operands = filters.stream().flatMap(filter -> filter instanceof And and ? and.operands().stream()
        : Stream.of(filter)).toList();
    return operands.size() == 1 ? operands.getFirst() : new And(operands);
  }

  /**
   * Matches rows that match at least one filter. With no filters, it matches no rows.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter or(Filter... filters) {
    return new Or(List.of(filters));
  }

  /**
   * Matches rows that do not match a filter.
   *
   * @param filter the filter
   * @return the filter
   */
  static Filter not(Filter filter) {
    return new Not(filter);
  }

  /**
   * Matches rows for which a query returns at least one row. Use {@link Query#on} to relate the query to the current
   * row, for example {@code exists(items.query().on("order_id", "id").where(gt("qty", 0)))}.
   *
   * @param rows the query, from {@link CollectionReference#query()}
   * @return the filter
   */
  static Filter exists(Query rows) {
    return new Exists(rows.statement());
  }

  /**
   * Matches rows for which a query returns no rows. Use {@link Query#on} to relate the query to the current row.
   *
   * @param rows the query, from {@link CollectionReference#query()}
   * @return the filter
   */
  static Filter notExists(Query rows) {
    return not(exists(rows));
  }
}
