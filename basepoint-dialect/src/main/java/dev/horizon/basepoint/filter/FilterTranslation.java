package dev.horizon.basepoint.filter;

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
import dev.horizon.aquery.aip160.Filter.Value;
import dev.horizon.aquery.aip160.InvalidFilterException;
import dev.horizon.aquery.aip160.Operator;
import dev.horizon.basepoint.expression.Expression;
import dev.horizon.basepoint.expression.Null;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class FilterTranslation {

  private static final Pattern INTEGER = Pattern.compile("-?\\d+");
  private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
  private static final Map<Operator, Compare.Operator> ORDER = Map.of(Operator.LESS_THAN,
      Compare.Operator.LT, Operator.LESS_EQUALS, Compare.Operator.LTE, Operator.GREATER_THAN,
      Compare.Operator.GT, Operator.GREATER_EQUALS, Compare.Operator.GTE);

  private final Set<String> relations;

  FilterTranslation(Set<String> relations) {
    this.relations = Set.copyOf(relations);
  }

  Filter translate(Condition condition) {
    return switch (condition) {
      case dev.horizon.aquery.aip160.Filter.And and -> new And(and.operands().stream().map(this::translate)
          .toList());
      case dev.horizon.aquery.aip160.Filter.Or or -> new Or(or.operands().stream().map(this::translate)
          .toList());
      case dev.horizon.aquery.aip160.Filter.Not not -> new Not(translate(not.operand()));
      case Restriction restriction -> restrict(restriction);
      case Global global -> throw new InvalidFilterException(
          "The filter term '%s' has no column. Compare a column with a value, for example state = OPEN.",
          global.value());
      default -> throw new InvalidFilterException("Unsupported filter condition: %s.",
          condition.getClass().getSimpleName());
    };
  }

  private Filter restrict(Restriction restriction) {
    List<String> segments = restriction.fieldPath().segments();
    Operator operator = restriction.operator();
    Value value = restriction.value();
    if (segments.size() == 2 && relations.contains(segments.getFirst())) {
      if (operator == Operator.HAS && !matchesAny(value)) {
        throw new InvalidFilterException("Unsupported filter '%s:%s'. %s is a relation of the query, and a column of a"
            + " relation supports only ':*', which matches the rows whose related row has a value in it.",
            String.join(".", segments), readText(value), segments.getFirst());
      }
      return restrictColumn(Expression.column(segments.getFirst(), segments.get(1)), operator, value);
    }
    if (segments.size() == 2) {
      return restrictKey(readKey(segments.getFirst(), segments.get(1)), operator, value);
    }
    if (segments.size() != 1) {
      throw new InvalidFilterException("Unsupported field path '%s'. Use a column name, a column and a key of the"
          + " JSON object in it, such as labels.env, or a relation and its column, such as customer.region.",
          String.join(".", segments));
    }
    if (operator == Operator.HAS && !matchesAny(value)) {
      return Filter.isNotNull(readKey(segments.getFirst(), readText(value)));
    }
    return restrictColumn(Expression.column(segments.getFirst()), operator, value);
  }

  private Filter restrictColumn(Expression column, Operator operator, Value value) {
    return switch (operator) {
      case HAS -> Filter.isNotNull(column);
      case STARTS_WITH -> Filter.startsWith(column, readText(value));
      case ENDS_WITH -> Filter.endsWith(column, readText(value));
      case EQUALS, NOT_EQUALS -> equal(column, operator == Operator.NOT_EQUALS, value, readLiteral(value));
      case LESS_THAN, LESS_EQUALS, GREATER_THAN, GREATER_EQUALS -> compare(column, operator, readLiteral(value));
      default -> throw new InvalidFilterException("Unsupported operator '%s'.", operator.symbol());
    };
  }

  private Filter restrictKey(Expression key, Operator operator, Value value) {
    return switch (operator) {
      case HAS -> {
        if (!matchesAny(value)) {
          throw new InvalidFilterException("Unsupported filter '%s'. A key of a JSON object supports only ':*', which"
              + " matches rows whose object has the key.", value.input());
        }
        yield Filter.isNotNull(key);
      }
      case STARTS_WITH -> Filter.startsWith(key, readText(value));
      case ENDS_WITH -> Filter.endsWith(key, readText(value));
      case EQUALS, NOT_EQUALS -> equal(key, operator == Operator.NOT_EQUALS, value, Expression.value(readText(value)));
      default -> throw new InvalidFilterException("Unsupported operator '%s' on a key of a JSON object, whose values"
          + " compare only as text.", operator.symbol());
    };
  }

  private Filter equal(Expression left, boolean negated, Value value, Expression right) {
    if (value instanceof StringLiteral string && string.wildcards() && string.value().contains("*")) {
      return new Match(left, List.of(string.value().split("\\*", -1)), negated);
    }
    return negated ? Filter.ne(left, right) : Filter.eq(left, right);
  }

  private Filter compare(Expression column, Operator operator, Expression right) {
    if (right instanceof Null) {
      throw new InvalidFilterException("Cannot compare null with %s. Use = or != with null.", operator.symbol());
    }
    return new Compare(column, ORDER.get(operator), right);
  }

  private Expression readKey(String column, String key) {
    try {
      return Expression.key(column, key);
    } catch (IllegalArgumentException invalid) {
      throw new InvalidFilterException("Unsupported key '%s' of the column %s. %s", key, column, invalid.getMessage());
    }
  }

  private boolean matchesAny(Value value) {
    return value instanceof Text text && text.text().equals("*");
  }

  private String readText(Value value) {
    return switch (value) {
      case StringLiteral string -> string.value();
      case Text text -> text.text();
      default -> value.input();
    };
  }

  private Expression readLiteral(Value value) {
    return switch (value) {
      case StringLiteral string -> Expression.value(string.value());
      case BoolLiteral bool -> Expression.value(bool.value());
      case IntLiteral integer -> Expression.value(integer.value());
      case DoubleLiteral decimal -> Expression.value(decimal.value());
      case TimestampLiteral timestamp -> Expression.value(timestamp.value());
      case DurationLiteral duration -> Expression.value(duration.value());
      case Text text -> parseText(text.text());
      default -> throw new InvalidFilterException("Unsupported value '%s'.", value.input());
    };
  }

  private Expression parseText(String text) {
    if (text.equals("null")) {
      return new Null();
    } else if (text.equals("true") || text.equals("false")) {
      return Expression.value(Boolean.parseBoolean(text));
    } else if (INTEGER.matcher(text).matches()) {
      BigInteger number = new BigInteger(text);
      return Expression.value(number.bitLength() < 64 ? (Object) number.longValueExact() : new BigDecimal(number));
    } else if (DECIMAL.matcher(text).matches()) {
      return Expression.value(new BigDecimal(text));
    }
    return Expression.value(text);
  }
}
