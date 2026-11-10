package dev.horizon.basepoint.filter;

import dev.horizon.basepoint.statement.StatementWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A filter in the AIP-160 filter syntax, translated into filters where it renders. A field of two parts, such as
 * {@code customer.region}, is a column of a relation when the statement has a relation of the first part, and a key
 * of the JSON object in a column otherwise. Empty text matches every row.
 *
 * @param parsed the parsed filter text
 */
public record Text(dev.horizon.aquery.aip160.Filter parsed) implements Filter {

  public Text {
    Objects.requireNonNull(parsed, "parsed");
  }

  /**
   * Translates the text into filters.
   *
   * @param relations the names of the relations that fields of two parts can read
   * @return the filter
   * @throws dev.horizon.aquery.aip160.InvalidFilterException if the text uses a field, an operator or a value that
   * has no translation
   */
  public Filter translate(Set<String> relations) {
    return parsed.condition() == null
        ? new And(List.of())
        : new FilterTranslation(relations).translate(parsed.condition());
  }

  @Override
  public void render(StatementWriter sql) {
    sql.render(translate(sql.relations()));
  }
}
