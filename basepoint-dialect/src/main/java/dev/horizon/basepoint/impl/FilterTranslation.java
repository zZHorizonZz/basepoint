package dev.horizon.basepoint.impl;

import dev.horizon.aquery.aip160.Filter.BoolLiteral;
import dev.horizon.aquery.aip160.Filter.Condition;
import dev.horizon.aquery.aip160.Filter.DoubleLiteral;
import dev.horizon.aquery.aip160.Filter.DurationLiteral;
import dev.horizon.aquery.aip160.Filter.Global;
import dev.horizon.aquery.aip160.Filter.IntLiteral;
import dev.horizon.aquery.aip160.Filter.Restriction;
import dev.horizon.aquery.aip160.Filter.StringLiteral;
import dev.horizon.aquery.aip160.Filter.Text;
import dev.horizon.aquery.aip160.Filter.TimestampLiteral;
import dev.horizon.aquery.aip160.InvalidFilterException;
import dev.horizon.aquery.aip160.Operator;
import dev.horizon.basepoint.Expression;
import dev.horizon.basepoint.Filter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts a condition from an aquery parser into a {@link Filter}.
 *
 * <p>
 * Each field becomes a column. An unquoted value becomes a {@code Long}, a {@code BigDecimal}, a {@code Boolean} or
 * {@code null} when its text has that form, and a {@code String} otherwise.
 */
public final class FilterTranslation {

  private static final Pattern INTEGER = Pattern.compile("-?\\d+");
  private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
  private static final Object NULL = new Object();

  public Filter translate(Condition condition) {
    return switch (condition) {
      case dev.horizon.aquery.aip160.Filter.And and -> new Filter.And(all(and.operands()));
      case dev.horizon.aquery.aip160.Filter.Or or -> new Filter.Or(all(or.operands()));
      case dev.horizon.aquery.aip160.Filter.Not not -> new Filter.Not(translate(not.operand()));
      case Restriction restriction -> restriction(restriction);
      case Global global -> throw new InvalidFilterException(
          "The filter term '%s' has no column. Compare a column with a value, for example state = OPEN.",
          global.value());
      default -> throw new InvalidFilterException("Unsupported filter condition: %s.",
          condition.getClass().getSimpleName());
    };
  }

  private List<Filter> all(List<Condition> conditions) {
    List<Filter> filters = new ArrayList<>(conditions.size());
    for (Condition condition : conditions) {
      filters.add(translate(condition));
    }
    return filters;
  }

  private Filter restriction(Restriction restriction) {
    List<String> segments = restriction.fieldPath().segments();
    if (segments.size() != 1) {
      throw new InvalidFilterException("Unsupported field path '%s'. Use a single column name.",
          String.join(".", segments));
    }
    Expression column = Expression.column(segments.getFirst());
    Operator operator = restriction.operator();
    dev.horizon.aquery.aip160.Filter.Value value = restriction.value();
    return switch (operator) {
      case HAS -> {
        if (value instanceof Text text && text.text().equals("*")) {
          yield new Filter.IsNull(column, true);
        }
        throw new InvalidFilterException("Unsupported filter '%s:%s'. The has operator supports only '%s:*', which"
            + " matches rows where the column is not null.", segments.getFirst(), value.input(), segments.getFirst());
      }
      case STARTS_WITH -> new Filter.Match(column, List.of(string(value), ""), false);
      case ENDS_WITH -> new Filter.Match(column, List.of("", string(value)), false);
      case EQUALS, NOT_EQUALS -> equality(column, operator == Operator.NOT_EQUALS, value);
      case LESS_THAN -> ordered(column, "<", value);
      case LESS_EQUALS -> ordered(column, "<=", value);
      case GREATER_THAN -> ordered(column, ">", value);
      case GREATER_EQUALS -> ordered(column, ">=", value);
      default -> throw new InvalidFilterException("Unsupported operator '%s'.", operator.symbol());
    };
  }

  private Filter equality(Expression column, boolean negated, dev.horizon.aquery.aip160.Filter.Value value) {
    if (value instanceof StringLiteral string && string.wildcards() && string.value().contains("*")) {
      return new Filter.Match(column, List.of(string.value().split("\\*", -1)), negated);
    }
    Object literal = literal(value);
    if (literal == NULL) {
      return new Filter.IsNull(column, negated);
    }
    return new Filter.Compare(column, negated ? "<>" : "=", Expression.value(literal));
  }

  private Filter ordered(Expression column, String operator, dev.horizon.aquery.aip160.Filter.Value value) {
    Object literal = literal(value);
    if (literal == NULL) {
      throw new InvalidFilterException("Cannot compare null with %s. Use = or != with null.", operator);
    }
    return new Filter.Compare(column, operator, Expression.value(literal));
  }

  private String string(dev.horizon.aquery.aip160.Filter.Value value) {
    return switch (value) {
      case StringLiteral string -> string.value();
      case Text text -> text.text();
      default -> value.input();
    };
  }

  /** Returns the value of a literal, or {@link #NULL} for {@code null}. */
  private Object literal(dev.horizon.aquery.aip160.Filter.Value value) {
    return switch (value) {
      case StringLiteral string -> string.value();
      case BoolLiteral bool -> bool.value();
      case IntLiteral integer -> integer.value();
      case DoubleLiteral decimal -> decimal.value();
      case TimestampLiteral timestamp -> timestamp.value();
      case DurationLiteral duration -> duration.value();
      case Text text -> text(text.text());
      default -> throw new InvalidFilterException("Unsupported value '%s'.", value.input());
    };
  }

  /** Returns an unquoted value as a number, a boolean or {@link #NULL} if it has that form, else as text. */
  private Object text(String text) {
    if (text.equals("null")) {
      return NULL;
    } else if (text.equals("true") || text.equals("false")) {
      return Boolean.parseBoolean(text);
    } else if (INTEGER.matcher(text).matches()) {
      BigInteger number = new BigInteger(text);
      return number.bitLength() < 64 ? (Object) number.longValueExact() : new BigDecimal(number);
    } else if (DECIMAL.matcher(text).matches()) {
      return new BigDecimal(text);
    }
    return text;
  }
}
