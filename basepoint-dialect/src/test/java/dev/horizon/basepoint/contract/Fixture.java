package dev.horizon.basepoint.contract;

import dev.horizon.basepoint.Dialect;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Fixture(Dialect dialect, Map<String, String> types) {

  private static final Pattern NAME = Pattern.compile("\\[\\[(.+?)]]");
  private static final Pattern TABLE = Pattern.compile("^CREATE TABLE (.+?) \\(");

  void load(Connection connection) throws SQLException {
    List<String> statements = statements();
    try (Statement statement = connection.createStatement()) {
      for (String sql : statements.reversed()) {
        Matcher table = TABLE.matcher(sql);
        if (table.find()) {
          statement.execute("DROP TABLE IF EXISTS " + table.group(1));
        }
      }
      for (String sql : statements) {
        statement.execute(sql);
      }
    }
  }

  private List<String> statements() {
    String text = read();
    for (Map.Entry<String, String> type : types.entrySet()) {
      text = text.replace("{" + type.getKey() + "}", type.getValue());
    }
    text = NAME.matcher(text).replaceAll(name -> Matcher.quoteReplacement(dialect.quote(name.group(1))));
    List<String> statements = new ArrayList<>();
    StringBuilder statement = new StringBuilder();
    for (String line : text.split("\n")) {
      if (line.startsWith("--") || line.isBlank()) {
        continue;
      }
      statement.append(line.strip()).append(' ');
      if (line.stripTrailing().endsWith(";")) {
        statements.add(statement.toString().strip().replaceAll(";$", ""));
        statement.setLength(0);
      }
    }
    return statements;
  }

  private String read() {
    try (InputStream input = Fixture.class.getResourceAsStream("/fixture.sql")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r", "");
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }
}
