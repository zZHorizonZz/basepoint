package dev.horizon.basepoint.contract;

import dev.horizon.aquery.cel.filter.CelFilterParser;
import dev.horizon.basepoint.filter.Filter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FilterCases {

  private static final Pattern FILTER = Pattern.compile("(filter|cel) (\\w+)(?:\\((\\w+)\\))?:(.*)");

  private final List<RowCase> rows = new ArrayList<>();
  private final List<ErrorCase> errors = new ArrayList<>();

  public FilterCases() {
    try {
      URI uri = FilterCases.class.getResource("/filters").toURI();
      if (uri.getScheme().equals("jar")) {
        try (FileSystem jar = FileSystems.newFileSystem(uri, Map.of())) {
          readAll(jar.getPath("/filters"));
        }
      } else {
        readAll(Path.of(uri));
      }
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    } catch (URISyntaxException failure) {
      throw new IllegalStateException(failure);
    }
  }

  public enum Syntax {
    AIP160 {
      @Override
      public Filter parse(String text) {
        return Filter.of(text);
      }
    },
    CEL {
      @Override
      public Filter parse(String text) {
        return Filter.of(text, new CelFilterParser());
      }
    };

    public abstract Filter parse(String text);
  }

  public record RowCase(String name, Syntax syntax, String table, String key, String text, List<String> rows) {

    @Override
    public String toString() {
      return name;
    }
  }

  public record ErrorCase(String name, Syntax syntax, String table, String text, String message) {

    @Override
    public String toString() {
      return name;
    }
  }

  public List<RowCase> rows() {
    return rows;
  }

  public List<ErrorCase> errors() {
    return errors;
  }

  private void readAll(Path folder) throws IOException {
    try (Stream<Path> files = Files.list(folder)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".txt")).sorted().toList()) {
        read(file);
      }
    }
  }

  private void read(Path file) throws IOException {
    String name = file.getFileName().toString().replace(".txt", "");
    List<String> lines = Arrays.asList(Files.readString(file).replace("\r", "").split("\n", -1));
    int start = 0;
    for (int i = 0; i <= lines.size(); i++) {
      if (i == lines.size() || lines.get(i).isBlank()) {
        if (i > start) {
          add(name + ":" + (start + 1), lines.subList(start, i));
        }
        start = i + 1;
      }
    }
  }

  private void add(String place, List<String> lines) {
    if (lines.size() != 3) {
      throw new IllegalStateException(place + ": a case is a description, a filter and a result, but has "
          + lines.size() + " lines.");
    }
    Matcher filter = FILTER.matcher(lines.get(1));
    if (!filter.matches()) {
      throw new IllegalStateException(place + ": the second line is not 'filter <table>: <text>' or 'cel <table>:"
          + " <text>', but " + lines.get(1));
    }
    String name = place + " " + lines.getFirst();
    Syntax syntax = filter.group(1).equals("cel") ? Syntax.CEL : Syntax.AIP160;
    String text = filter.group(4).strip();
    String outcome = lines.get(2);
    if (outcome.startsWith("rows:")) {
      String values = outcome.substring("rows:".length()).strip();
      rows.add(new RowCase(name, syntax, filter.group(2), filter.group(3) == null ? "id" : filter.group(3), text,
          values.isEmpty() ? List.of() : Arrays.stream(values.split(",")).map(String::strip).toList()));
    } else if (outcome.startsWith("error:")) {
      errors.add(new ErrorCase(name, syntax, filter.group(2), text, outcome.substring("error:".length()).strip()));
    } else {
      throw new IllegalStateException(place + ": the third line is not 'rows: ...' or 'error: ...', but " + outcome);
    }
  }
}
