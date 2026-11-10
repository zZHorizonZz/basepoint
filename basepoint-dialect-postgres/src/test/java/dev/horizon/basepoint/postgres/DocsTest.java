package dev.horizon.basepoint.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.RenderedStatement;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("The docs")
class DocsTest {

  private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)\\)");
  private static final Pattern DECLARATION = Pattern.compile("(?s)\\s*([A-Z][\\w.]*(?:<[^=;]*>)?(?:\\[])?)\\s+(\\w+)"
      + "\\s*=(.*)");
  private static final Pattern ASSIGNMENT = Pattern.compile("(?s)(\\w+)\\s*(=(?!=)|\\.).*");
  private static final Pattern RENDERS = Pattern.compile("(?i).*\\b(renders|returns this SQL)\\b.*:$");
  private static final List<String> IMPORTS = List.of("import static dev.horizon.basepoint.expression.Expression.*;",
      "import static dev.horizon.basepoint.filter.Filter.*;", "import dev.horizon.basepoint.*;",
      "import dev.horizon.basepoint.expression.*;", "import dev.horizon.basepoint.filter.*;",
      "import dev.horizon.basepoint.statement.*;", "import dev.horizon.basepoint.postgres.*;",
      "import dev.horizon.basepoint.mariadb.*;", "import dev.horizon.basepoint.sqlserver.*;",
      "import dev.horizon.basepoint.sqlite.*;", "import dev.horizon.aquery.ParameterStyle;",
      "import dev.horizon.aquery.cel.filter.CelFilterParser;", "import java.math.BigDecimal;",
      "import java.time.Duration;", "import java.time.Instant;", "import java.util.Arrays;", "import java.util.List;",
      "import java.util.Map;",
      "import java.util.LinkedHashMap;", "import java.sql.Connection;", "import java.sql.PreparedStatement;",
      "import java.sql.ResultSet;");

  private final Path root = Path.of("..").toAbsolutePath().normalize();
  private final ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow();

  @TempDir
  private Path classes;

  record Example(String name, String code, String sql) {

    @Override
    public String toString() {
      return name;
    }
  }

  List<Path> pages() throws IOException {
    List<Path> pages = new ArrayList<>(List.of(root.resolve("README.md")));
    try (Stream<Path> docs = Files.walk(root.resolve("docs"))) {
      docs.filter(path -> path.toString().endsWith(".md")).sorted().forEach(pages::add);
    }
    return pages;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pages")
  @DisplayName("Link to pages and headings that exist")
  void links(Path page) throws IOException {
    List<String> broken = new ArrayList<>();
    for (String line : prose(page)) {
      Matcher link = LINK.matcher(line);
      while (link.find()) {
        String target = link.group(1);
        if (target.startsWith("http") || target.startsWith("mailto:")) {
          continue;
        }
        String[] parts = target.split("#", 2);
        Path file = parts[0].isEmpty() ? page : page.getParent().resolve(parts[0]).normalize();
        if (!Files.exists(file)) {
          broken.add(target + " (no file)");
        } else if (parts.length == 2 && file.toString().endsWith(".md") && !anchors(file).contains(parts[1])) {
          broken.add(target + " (no heading)");
        }
      }
    }
    assertThat(broken).as(root.relativize(page).toString()).isEmpty();
  }

  List<Example> examples() throws IOException {
    List<Example> examples = new ArrayList<>();
    for (Path page : pages()) {
      examples.addAll(examples(page));
    }
    return examples;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("examples")
  @DisplayName("Have Java examples that compile and render the SQL after them")
  void example(Example example) throws Exception {
    String name = "Example" + Math.abs(example.name().hashCode());
    Path source = classes.resolve(name + ".java");
    Files.writeString(source, example.code().replace("class Example ", "class " + name + " "));
    String classpath = System.getProperty("java.class.path") + File.pathSeparator + System.getProperty(
        "jdk.module.path", "");
    StringWriter errors = new StringWriter();
    int status = javac.run(new PrintWriter(errors), new PrintWriter(errors), "-d", classes.toString(), "-cp",
        classpath, "-proc:none", "-nowarn", source.toString());
    assertThat(errors.toString()).as("%s%n%s", example.name(), example.code()).doesNotContain("error:");
    assertThat(status).isZero();
    if (example.sql().isEmpty()) {
      return;
    }
    try (URLClassLoader loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, getClass()
        .getClassLoader())) {
      Object instance = loader.loadClass(name).getDeclaredConstructor().newInstance();
      Object result = instance.getClass().getMethod("run").invoke(instance);
      RenderedStatement statement = result instanceof RenderedStatement rendered
          ? rendered
          : (RenderedStatement) result.getClass().getMethod("build").invoke(result);
      assertThat(normalize(statement.text())).as(example.name()).isEqualTo(normalize(example.sql()));
    }
  }

  private String normalize(String sql) {
    return sql.replaceAll("\\$\\d+|@p\\d+", "?").replaceAll("\\s+", " ").replace("( ", "(").replace(" )", ")")
        .strip();
  }

  private List<Example> examples(Path page) throws IOException {
    List<String> lines = Files.readAllLines(page);
    List<Example> examples = new ArrayList<>();
    Map<String, String> fields = defaults();
    Set<String> imports = new LinkedHashSet<>(IMPORTS);
    for (int i = 0; i < lines.size(); i++) {
      if (!lines.get(i).strip().equals("```java")) {
        continue;
      }
      int start = i;
      String indent = lines.get(i).substring(0, lines.get(i).indexOf('`'));
      List<String> code = new ArrayList<>();
      for (i++; !lines.get(i).strip().equals("```"); i++) {
        code.add(lines.get(i).startsWith(indent) ? lines.get(i).substring(indent.length()) : lines.get(i));
      }
      String sql = sqlAfter(lines, i + 1);
      List<String> body = new ArrayList<>();
      for (String line : code) {
        if (line.startsWith("import ")) {
          imports.add(line.strip());
        } else {
          body.add(line.replaceAll(";\\s*//.*$", ";"));
        }
      }
      List<String> statements = statements(String.join("\n", body));
      examples.add(new Example(root.relativize(page) + ":" + (start + 1), source(imports, fields, statements,
          !sql.isEmpty()), sql));
      for (String statement : statements) {
        Matcher declaration = DECLARATION.matcher(statement);
        Matcher assignment = ASSIGNMENT.matcher(statement);
        if (declaration.matches()) {
          String variable = declaration.group(2);
          fields.keySet().removeIf(key -> key.equals(variable) || key.startsWith(variable + "#"));
          fields.put(variable, declaration.group(1) + " " + variable + " =" + declaration.group(3) + ";");
        } else if (assignment.matches() && fields.containsKey(assignment.group(1))) {
          fields.put(assignment.group(1) + "#" + fields.size(), "{ " + statement + "; }");
        }
      }
    }
    return examples;
  }

  private String sqlAfter(List<String> lines, int from) {
    List<String> prose = new ArrayList<>();
    for (int i = from; i < lines.size(); i++) {
      String line = lines.get(i).strip();
      if (line.equals("```sql")) {
        if (prose.size() != 1 || !RENDERS.matcher(prose.getFirst()).matches()) {
          return "";
        }
        StringBuilder sql = new StringBuilder();
        for (i++; !lines.get(i).strip().equals("```"); i++) {
          sql.append(lines.get(i).strip()).append(' ');
        }
        return sql.toString();
      }
      if (line.startsWith("```") || line.startsWith("#")) {
        return "";
      }
      if (!line.isEmpty()) {
        prose.add(line);
      }
    }
    return "";
  }

  private List<String> statements(String code) {
    List<String> statements = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < code.length(); i++) {
      char c = code.charAt(i);
      if (c == '"') {
        i = code.indexOf('"', code.startsWith("\"\"\"", i) ? code.indexOf("\"\"\"", i + 3) + 2 : i + 1);
        while (code.charAt(i - 1) == '\\' && code.charAt(i - 2) != '\\') {
          i = code.indexOf('"', i + 1);
        }
      } else if (c == '\'') {
        i = code.indexOf('\'', code.charAt(i + 1) == '\\' ? i + 3 : i + 2);
      } else if (c == '(' || c == '{' || c == '[') {
        depth++;
      } else if (c == ')' || c == '}' || c == ']') {
        depth--;
        if (depth == 0 && c == '}' && code.substring(i + 1).stripLeading().isEmpty()) {
          statements.add(code.substring(start, i + 1).strip());
          start = i + 1;
        }
      } else if (c == ';' && depth == 0) {
        statements.add(code.substring(start, i).strip());
        start = i + 1;
      }
    }
    if (!code.substring(start).isBlank()) {
      statements.add(code.substring(start).strip());
    }
    return statements.stream().filter(statement -> !statement.isEmpty()).toList();
  }

  private String source(Set<String> imports, Map<String, String> fields, List<String> statements, boolean renders) {
    StringBuilder source = new StringBuilder(String.join("\n", imports)).append("\n\npublic class Example {\n");
    fields.values().forEach(field -> source.append("  ").append(field).append('\n'));
    source.append("\n  public Object run() throws Exception {\n");
    for (int i = 0; i < statements.size(); i++) {
      String statement = statements.get(i);
      boolean block = statement.endsWith("}") && !statement.contains("->");
      if (i == statements.size() - 1 && renders) {
        Matcher declaration = DECLARATION.matcher(statement);
        source.append(declaration.matches()
            ? "    " + statement + ";\n    return " + declaration.group(2) + ";\n"
            : "    return " + statement + ";\n");
        return source.append("  }\n}\n").toString();
      }
      source.append("    ").append(statement).append(block ? "\n" : ";\n");
    }
    return source.append("    return null;\n  }\n}\n").toString();
  }

  private List<String> prose(Path page) throws IOException {
    List<String> prose = new ArrayList<>();
    boolean code = false;
    for (String line : Files.readAllLines(page)) {
      if (line.strip().startsWith("```")) {
        code = !code;
      } else if (!code) {
        prose.add(line.replaceAll("`[^`]*`", ""));
      }
    }
    return prose;
  }

  private Set<String> anchors(Path page) throws IOException {
    Set<String> anchors = new LinkedHashSet<>();
    boolean code = false;
    for (String line : Files.readAllLines(page)) {
      code = line.strip().startsWith("```") != code;
      if (!code && line.startsWith("#")) {
        anchors.add(line.replaceFirst("^#+\\s*", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 _-]", "")
            .replace(' ', '-'));
      }
    }
    return anchors;
  }

  private Map<String, String> defaults() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("dialect", "Dialect dialect = new PostgresDialect();");
    for (String table : List.of("orders", "items", "products", "customers", "archived", "defaults", "labelled",
        "categories", "events")) {
      fields.put(table, "CollectionReference " + table + " = new PostgresDialect().collection(\"" + table + "\");");
    }
    fields.put("connection", "Connection connection = null;");
    return fields;
  }
}
