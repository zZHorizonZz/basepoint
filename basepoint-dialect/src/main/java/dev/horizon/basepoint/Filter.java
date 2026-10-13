package dev.horizon.basepoint;

import dev.horizon.aquery.aip160.FilterParser;
import dev.horizon.aquery.ebnf.EbnfFilterParser;
import dev.horizon.basepoint.impl.FilterTranslation;
import dev.horizon.basepoint.impl.QueryImpl;
import java.util.Collection;
import java.util.List;

/**
 * A condition of rows, for WHERE and HAVING.
 *
 * <p>
 * The filters of this interface are built in Java, as those of the MongoDB driver are, or parsed from text with
 * {@link #of(String)}; both are the same filters, and they compose. A value that a filter takes is always bound, never
 * written into the SQL. A filter writes itself, thus a library or an application adds its own by implementing this
 * interface.
 *
 * <p>
 * Strings compare as the database compares them, by the collation of the column.
 */
public interface Filter {

  /**
   * Writes the filter, as a condition.
   *
   * @param sql the writer of the statement
   */
  void write(StatementWriter sql);

  /**
   * Parses a filter of the EBNF grammar of AIP-160, such as {@code state = "OPEN" AND total > 100}.
   *
   * @param text the filter
   * @return the filter
   * @throws dev.horizon.aquery.InvalidQueryException if the text is not a filter that basepoint can write
   * @see #of(String, FilterParser)
   */
  static Filter of(String text) {
    return of(text, new EbnfFilterParser());
  }

  /**
   * Parses a filter with a parser of aquery, such as the one of CEL.
   *
   * <p>
   * A field is a column; a path of fields is refused, since basepoint knows no schema to read it. A value without
   * quotes is a number, {@code true}, {@code false} or {@code null} when it reads as one, and a string otherwise; a
   * value in quotes is a string, and its {@code *} matches any text. {@code field:*} tells that a column is not null.
   *
   * @param text the filter
   * @param parser the parser of the grammar
   * @return the filter
   * @throws dev.horizon.aquery.InvalidQueryException if the text is not a filter that basepoint can write
   */
  static Filter of(String text, FilterParser parser) {
    return new FilterTranslation().translate(parser.parse(text).condition());
  }

  /**
   * Tells that a column equals a value; with null, that it is null.
   *
   * @param column the name of the column
   * @param value the value, an {@link Expression} or null
   * @return the filter
   */
  static Filter eq(String column, Object value) {
    return eq(Expression.column(column), value);
  }

  /**
   * Tells that an expression equals a value; with null, that it is null.
   *
   * @param left the expression
   * @param value the value, an {@link Expression} or null
   * @return the filter
   */
  static Filter eq(Expression left, Object value) {
    return value == null ? new IsNull(left, false) : new Compare(left, "=", Expression.value(value));
  }

  /**
   * Tells that a column differs from a value; with null, that it is not null. A column that is null differs from no
   * value.
   *
   * @param column the name of the column
   * @param value the value, an {@link Expression} or null
   * @return the filter
   */
  static Filter ne(String column, Object value) {
    return ne(Expression.column(column), value);
  }

  /**
   * Tells that an expression differs from a value; with null, that it is not null.
   *
   * @param left the expression
   * @param value the value, an {@link Expression} or null
   * @return the filter
   */
  static Filter ne(Expression left, Object value) {
    return value == null ? new IsNull(left, true) : new Compare(left, "<>", Expression.value(value));
  }

  /**
   * Tells that a column is greater than a value.
   *
   * @param column the name of the column
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gt(String column, Object value) {
    return gt(Expression.column(column), value);
  }

  /**
   * Tells that an expression is greater than a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gt(Expression left, Object value) {
    return new Compare(left, ">", Expression.value(value));
  }

  /**
   * Tells that a column is greater than or equals a value.
   *
   * @param column the name of the column
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gte(String column, Object value) {
    return gte(Expression.column(column), value);
  }

  /**
   * Tells that an expression is greater than or equals a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter gte(Expression left, Object value) {
    return new Compare(left, ">=", Expression.value(value));
  }

  /**
   * Tells that a column is less than a value.
   *
   * @param column the name of the column
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lt(String column, Object value) {
    return lt(Expression.column(column), value);
  }

  /**
   * Tells that an expression is less than a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lt(Expression left, Object value) {
    return new Compare(left, "<", Expression.value(value));
  }

  /**
   * Tells that a column is less than or equals a value.
   *
   * @param column the name of the column
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lte(String column, Object value) {
    return lte(Expression.column(column), value);
  }

  /**
   * Tells that an expression is less than or equals a value.
   *
   * @param left the expression
   * @param value the value or an {@link Expression}
   * @return the filter
   */
  static Filter lte(Expression left, Object value) {
    return new Compare(left, "<=", Expression.value(value));
  }

  /**
   * Tells that a column equals one of the values; no row matches no values.
   *
   * @param column the name of the column
   * @param values the values
   * @return the filter
   */
  static Filter in(String column, Collection<?> values) {
    return in(Expression.column(column), values);
  }

  /**
   * Tells that an expression equals one of the values; no row matches no values.
   *
   * @param left the expression
   * @param values the values
   * @return the filter
   */
  static Filter in(Expression left, Collection<?> values) {
    return new In(left, List.copyOf(values), false);
  }

  /**
   * Tells that a column equals none of the values; every row in which it is not null matches no values.
   *
   * @param column the name of the column
   * @param values the values
   * @return the filter
   */
  static Filter nin(String column, Collection<?> values) {
    return nin(Expression.column(column), values);
  }

  /**
   * Tells that an expression equals none of the values.
   *
   * @param left the expression
   * @param values the values
   * @return the filter
   */
  static Filter nin(Expression left, Collection<?> values) {
    return new In(left, List.copyOf(values), true);
  }

  /**
   * Tells that a column is between two values, both included.
   *
   * @param column the name of the column
   * @param low the least value
   * @param high the greatest value
   * @return the filter
   */
  static Filter between(String column, Object low, Object high) {
    return between(Expression.column(column), low, high);
  }

  /**
   * Tells that an expression is between two values, both included.
   *
   * @param left the expression
   * @param low the least value
   * @param high the greatest value
   * @return the filter
   */
  static Filter between(Expression left, Object low, Object high) {
    return new Between(left, Expression.value(low), Expression.value(high));
  }

  /**
   * Tells that a column is null.
   *
   * @param column the name of the column
   * @return the filter
   */
  static Filter isNull(String column) {
    return new IsNull(Expression.column(column), false);
  }

  /**
   * Tells that a column is not null.
   *
   * @param column the name of the column
   * @return the filter
   */
  static Filter isNotNull(String column) {
    return new IsNull(Expression.column(column), true);
  }

  /**
   * Tells that a column starts with a text, in which no character is a wildcard.
   *
   * @param column the name of the column
   * @param prefix the text
   * @return the filter
   */
  static Filter startsWith(String column, String prefix) {
    return new Match(Expression.column(column), List.of(prefix, ""), false);
  }

  /**
   * Tells that a column ends with a text, in which no character is a wildcard.
   *
   * @param column the name of the column
   * @param suffix the text
   * @return the filter
   */
  static Filter endsWith(String column, String suffix) {
    return new Match(Expression.column(column), List.of("", suffix), false);
  }

  /**
   * Tells that a column contains a text, in which no character is a wildcard.
   *
   * @param column the name of the column
   * @param part the text
   * @return the filter
   */
  static Filter contains(String column, String part) {
    return new Match(Expression.column(column), List.of("", part, ""), false);
  }

  /**
   * Tells that a column matches a pattern of LIKE, whose wildcards are those of the database.
   *
   * @param column the name of the column
   * @param pattern the pattern
   * @return the filter
   */
  static Filter like(String column, String pattern) {
    return new Like(Expression.column(column), pattern);
  }

  /**
   * Tells that all filters match; with none, every row matches.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter and(Filter... filters) {
    return new And(List.of(filters));
  }

  /**
   * Tells that one of the filters matches; with none, no row matches.
   *
   * @param filters the filters
   * @return the filter
   */
  static Filter or(Filter... filters) {
    return new Or(List.of(filters));
  }

  /**
   * Tells that a filter does not match.
   *
   * @param filter the filter
   * @return the filter
   */
  static Filter not(Filter filter) {
    return new Not(filter);
  }

  /**
   * Tells that a query has rows, such as the related rows of an edge:
   * {@code exists(items.query().on("order_id", "id").where(gt("qty", 0)))}.
   *
   * @param rows the rows, which {@link Query#on} relates to the row around them
   * @return the filter
   */
  static Filter exists(Query rows) {
    return new Exists(rows);
  }

  /**
   * Tells that a query has no rows, such as the related rows of an edge.
   *
   * @param rows the rows, which {@link Query#on} relates to the row around them
   * @return the filter
   */
  static Filter notExists(Query rows) {
    return not(exists(rows));
  }

  /**
   * A comparison of two values.
   *
   * @param left the left value
   * @param operator the operator of SQL, such as {@code <=}
   * @param right the right value
   */
  record Compare(Expression left, String operator, Expression right) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("(").write(left).append(" " + operator + " ").write(right).append(")");
    }
  }

  /**
   * Tells that a value is null, or that it is not.
   *
   * @param operand the value
   * @param negated true for IS NOT NULL
   */
  record IsNull(Expression operand, boolean negated) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("(").write(operand).append(negated ? " IS NOT NULL)" : " IS NULL)");
    }
  }

  /**
   * Tells that a value is one of some values, or that it is none of them.
   *
   * @param operand the value
   * @param values the values, which are bound
   * @param negated true for NOT IN
   */
  record In(Expression operand, List<?> values, boolean negated) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      if (values.isEmpty()) {
        sql.append(negated ? "(1 = 1)" : "(1 = 0)");
        return;
      }
      sql.append("(").write(operand).append(negated ? " NOT IN (" : " IN (");
      for (int i = 0; i < values.size(); i++) {
        sql.append(i == 0 ? "" : ", ").write(Expression.value(values.get(i)));
      }
      sql.append("))");
    }
  }

  /**
   * Tells that a value is between two values, both included.
   *
   * @param operand the value
   * @param low the least value
   * @param high the greatest value
   */
  record Between(Expression operand, Expression low, Expression high) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("(").write(operand).append(" BETWEEN ").write(low).append(" AND ").write(high).append(")");
    }
  }

  /**
   * Tells that a string is made of texts with any text between them: {@code ["ab", ""]} starts with {@code ab}. The
   * texts have no wildcards; the filter escapes those of the database.
   *
   * @param operand the string
   * @param parts the texts, which any text joins
   * @param negated true for NOT LIKE
   */
  record Match(Expression operand, List<String> parts, boolean negated) implements Filter {

    private static final char ESCAPE = '!';

    @Override
    public void write(StatementWriter sql) {
      String wildcards = sql.dialect().likeWildcards();
      StringBuilder pattern = new StringBuilder();
      for (int i = 0; i < parts.size(); i++) {
        if (i > 0) {
          pattern.append('%');
        }
        for (char c : parts.get(i).toCharArray()) {
          if (c == ESCAPE || wildcards.indexOf(c) >= 0) {
            pattern.append(ESCAPE);
          }
          pattern.append(c);
        }
      }
      sql.append("(").write(operand).append(negated ? " NOT LIKE " : " LIKE ").param(pattern.toString())
          .append(" ESCAPE '" + ESCAPE + "')");
    }
  }

  /**
   * Tells that a string matches a pattern of LIKE.
   *
   * @param operand the string
   * @param pattern the pattern, with the wildcards of the database
   */
  record Like(Expression operand, String pattern) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("(").write(operand).append(" LIKE ").param(pattern).append(")");
    }
  }

  /**
   * Filters that all match.
   *
   * @param operands the filters
   */
  record And(List<Filter> operands) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      junction(sql, operands, " AND ", "(1 = 1)");
    }
  }

  /**
   * Filters of which one matches.
   *
   * @param operands the filters
   */
  record Or(List<Filter> operands) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      junction(sql, operands, " OR ", "(1 = 0)");
    }
  }

  /**
   * A filter that does not match.
   *
   * @param operand the filter
   */
  record Not(Filter operand) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("(NOT ").write(operand).append(")");
    }
  }

  /**
   * Tells that the rows of a subquery are there.
   *
   * @param rows the rows
   */
  record Exists(Query rows) implements Filter {

    @Override
    public void write(StatementWriter sql) {
      sql.append("EXISTS (");
      QueryImpl.related(rows).writeExists(sql);
      sql.append(")");
    }
  }

  private static void junction(StatementWriter sql, List<Filter> operands, String operator, String empty) {
    if (operands.isEmpty()) {
      sql.append(empty);
      return;
    }
    sql.append("(");
    for (int i = 0; i < operands.size(); i++) {
      sql.append(i == 0 ? "" : operator).write(operands.get(i));
    }
    sql.append(")");
  }
}
