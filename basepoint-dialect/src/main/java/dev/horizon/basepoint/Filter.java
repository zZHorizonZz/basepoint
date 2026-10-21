package dev.horizon.basepoint;

import dev.horizon.aquery.aip160.FilterParser;
import dev.horizon.aquery.ebnf.EbnfFilterParser;
import dev.horizon.basepoint.impl.FilterTranslation;
import dev.horizon.basepoint.impl.QueryImpl;
import java.util.Collection;
import java.util.List;

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
  static Filter of(String text) {
    return of(text, new EbnfFilterParser());
  }

  /**
   * Parses a filter with an aquery parser, for example {@code new CelFilterParser()} for CEL.
   *
   * <p>
   * Each field is a column name, or a column name and a key of the JSON object in the column, such as
   * {@code labels.env}, whose values compare as text. Unquoted values become numbers, booleans or {@code null} when
   * they have that form, and strings otherwise. Quoted values are strings, and {@code *} inside them matches any text.
   * {@code column:*} matches rows where the column is not {@code NULL}, and {@code column:key} the rows whose object
   * has the key.
   *
   * @param text the filter text
   * @param parser the parser
   * @return the filter
   * @throws dev.horizon.aquery.InvalidQueryException if the text is not valid
   */
  static Filter of(String text, FilterParser parser) {
    return new FilterTranslation().translate(parser.parse(text).condition());
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
    return value == null ? new IsNull(left, false) : new Compare(left, "=", Expression.value(value));
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
    return value == null ? new IsNull(left, true) : new Compare(left, "<>", Expression.value(value));
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
    return new Compare(left, ">", Expression.value(value));
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
    return new Compare(left, ">=", Expression.value(value));
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
    return new Compare(left, "<", Expression.value(value));
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
    return new Compare(left, "<=", Expression.value(value));
  }

  /**
   * Matches rows where a column equals one of the values. An empty collection matches no rows.
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
    return new In(left, List.copyOf(values), false);
  }

  /**
   * Matches rows where a column equals none of the values. An empty collection matches every row. Rows where the
   * column is {@code NULL} match only an empty collection.
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
    return new In(left, List.copyOf(values), true);
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
    return new IsNull(Expression.column(column), false);
  }

  /**
   * Matches rows where a column is not {@code NULL}.
   *
   * @param column the column name
   * @return the filter
   */
  static Filter isNotNull(String column) {
    return new IsNull(Expression.column(column), true);
  }

  /**
   * Matches rows where a column starts with a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param prefix the text
   * @return the filter
   */
  static Filter startsWith(String column, String prefix) {
    return new Match(Expression.column(column), List.of(prefix, ""), false);
  }

  /**
   * Matches rows where a column ends with a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param suffix the text
   * @return the filter
   */
  static Filter endsWith(String column, String suffix) {
    return new Match(Expression.column(column), List.of("", suffix), false);
  }

  /**
   * Matches rows where a column contains a text. Wildcard characters in the text match only themselves.
   *
   * @param column the column name
   * @param part the text
   * @return the filter
   */
  static Filter contains(String column, String part) {
    return new Match(Expression.column(column), List.of("", part, ""), false);
  }

  /**
   * Matches rows where a column matches a {@code LIKE} pattern. The pattern uses the wildcards of the database.
   *
   * @param column the column name
   * @param pattern the pattern, for example {@code M%}
   * @return the filter
   */
  static Filter like(String column, String pattern) {
    return new Like(Expression.column(column), pattern);
  }

  /**
   * Matches rows that match all filters. With no filters, it matches every row.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter and(Filter... filters) {
    return new And(List.of(filters));
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
    return new Exists(QueryImpl.related(rows).exists());
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

  /**
   * A comparison: {@code (left operator right)}.
   *
   * @param left the left value
   * @param operator the SQL operator, for example {@code <=}
   * @param right the right value
   */
  record Compare(Expression left, String operator, Expression right) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(left).append(" " + operator + " ").render(right).append(")");
    }
  }

  /**
   * {@code IS NULL} or {@code IS NOT NULL}.
   *
   * @param operand the value to test
   * @param negated {@code true} to render {@code IS NOT NULL}
   */
  record IsNull(Expression operand, boolean negated) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(operand).append(negated ? " IS NOT NULL)" : " IS NULL)");
    }
  }

  /**
   * {@code IN} or {@code NOT IN}. An empty list renders {@code (1 = 0)} for {@code IN} and {@code (1 = 1)} for
   * {@code NOT IN}.
   *
   * @param operand the value to test
   * @param values the values, each bound as a placeholder
   * @param negated {@code true} to render {@code NOT IN}
   */
  record In(Expression operand, List<?> values, boolean negated) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      if (values.isEmpty()) {
        sql.append(negated ? "(1 = 1)" : "(1 = 0)");
        return;
      }
      sql.append("(").render(operand).append(negated ? " NOT IN (" : " IN (");
      for (int i = 0; i < values.size(); i++) {
        sql.append(i == 0 ? "" : ", ").render(Expression.value(values.get(i)));
      }
      sql.append("))");
    }
  }

  /**
   * {@code BETWEEN}, inclusive.
   *
   * @param operand the value to test
   * @param low the lower bound
   * @param high the upper bound
   */
  record Between(Expression operand, Expression low, Expression high) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(operand).append(" BETWEEN ").render(low).append(" AND ").render(high).append(")");
    }
  }

  /**
   * A {@code LIKE} match of literal text parts with any text between them. For example, {@code ["ab", ""]} matches
   * strings that start with {@code ab}. The filter escapes {@code %}, {@code _} and {@code [} in the parts with
   * {@code !}.
   *
   * @param operand the string to test
   * @param parts the literal parts
   * @param negated {@code true} to render {@code NOT LIKE}
   */
  record Match(Expression operand, List<String> parts, boolean negated) implements Filter {

    private static final char ESCAPE = '!';
    private static final String WILDCARDS = "%_[";

    @Override
    public void render(StatementWriter sql) {
      StringBuilder pattern = new StringBuilder();
      for (int i = 0; i < parts.size(); i++) {
        if (i > 0) {
          pattern.append('%');
        }
        for (char c : parts.get(i).toCharArray()) {
          if (c == ESCAPE || WILDCARDS.indexOf(c) >= 0) {
            pattern.append(ESCAPE);
          }
          pattern.append(c);
        }
      }
      sql.append("(").render(operand).append(negated ? " NOT LIKE " : " LIKE ").param(pattern.toString())
          .append(" ESCAPE '" + ESCAPE + "')");
    }
  }

  /**
   * {@code LIKE} with a pattern as it is.
   *
   * @param operand the string to test
   * @param pattern the pattern, with the wildcards of the database
   */
  record Like(Expression operand, String pattern) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").render(operand).append(" LIKE ").param(pattern).append(")");
    }
  }

  /**
   * {@code AND} of filters. An empty list renders {@code (1 = 1)}.
   *
   * @param operands the filters
   */
  record And(List<Filter> operands) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      junction(sql, operands, " AND ", "(1 = 1)");
    }
  }

  /**
   * {@code OR} of filters. An empty list renders {@code (1 = 0)}.
   *
   * @param operands the filters
   */
  record Or(List<Filter> operands) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      junction(sql, operands, " OR ", "(1 = 0)");
    }
  }

  /**
   * {@code NOT} of a filter.
   *
   * @param operand the filter
   */
  record Not(Filter operand) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(NOT ").render(operand).append(")");
    }
  }

  /**
   * {@code EXISTS} of a statement.
   *
   * @param rows the statement
   */
  record Exists(Statement rows) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("EXISTS (").render(rows).append(")");
    }
  }

  /**
   * Equality between a column of the current source and a column of the enclosing statement's source.
   *
   * @param column the column of the current source
   * @param outerColumn the column of the enclosing source
   */
  record Related(String column, String outerColumn) implements Filter {

    @Override
    public void render(StatementWriter sql) {
      sql.append("(").column(column).append(" = ").outerColumn(outerColumn).append(")");
    }
  }

  private static void junction(StatementWriter sql, List<Filter> operands, String operator, String empty) {
    if (operands.isEmpty()) {
      sql.append(empty);
      return;
    }
    sql.append("(");
    for (int i = 0; i < operands.size(); i++) {
      sql.append(i == 0 ? "" : operator).render(operands.get(i));
    }
    sql.append(")");
  }
}
