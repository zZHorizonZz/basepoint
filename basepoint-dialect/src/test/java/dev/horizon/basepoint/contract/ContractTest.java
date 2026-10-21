package dev.horizon.basepoint.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.horizon.basepoint.Buildable;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.RenderedStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  protected TestDatabase database;
  protected Dialect dialect;
  protected CollectionReference orders;
  protected CollectionReference items;
  protected CollectionReference products;
  protected CollectionReference archived;
  protected CollectionReference defaults;
  protected CollectionReference labelled;
  private Connection connection;

  protected abstract TestDatabase database();

  @BeforeAll
  void connect() throws SQLException {
    database = database();
    dialect = database.dialect();
    connection = database.connect();
    orders = dialect.collection("orders");
    items = dialect.collection("items");
    products = dialect.collection("products");
    archived = dialect.collection("archived");
    defaults = dialect.collection("defaults");
    labelled = dialect.collection("labelled");
  }

  @AfterAll
  void disconnect() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @BeforeEach
  void loadFixture() throws SQLException {
    Fixture.load(connection, database.jsonType());
  }

  protected List<Integer> ids(Query query) throws SQLException {
    return idsInOrder(query.orderBy("id"));
  }

  protected List<Integer> idsInOrder(Buildable builder) throws SQLException {
    return rows(builder).stream().map(row -> (int) number(row.get("id"))).toList();
  }

  protected List<Integer> sortedIds(Buildable builder) throws SQLException {
    List<Integer> ids = new ArrayList<>(idsInOrder(builder));
    ids.sort(null);
    return ids;
  }

  protected List<Map<String, Object>> rows(Buildable builder) throws SQLException {
    RenderedStatement statement = builder.build();
    try (PreparedStatement prepared = prepare(statement)) {
      boolean hasRows = prepared.execute();
      while (!hasRows && prepared.getUpdateCount() != -1) {
        hasRows = prepared.getMoreResults();
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      if (!hasRows) {
        return rows;
      }
      try (ResultSet result = prepared.getResultSet()) {
        ResultSetMetaData meta = result.getMetaData();
        while (result.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i).toLowerCase(), result.getObject(i));
          }
          rows.add(row);
        }
      }
      return rows;
    }
  }

  protected int execute(Buildable builder) throws SQLException {
    try (PreparedStatement prepared = prepare(builder.build())) {
      return prepared.executeUpdate();
    }
  }

  protected static long number(Object value) {
    return ((Number) value).longValue();
  }

  protected static JsonNode json(Object value) {
    try {
      return JSON.readTree(value.toString());
    } catch (Exception failed) {
      throw new IllegalArgumentException("The value is not JSON: " + value, failed);
    }
  }

  protected static Map<String, Object> order(int id, String state, int total) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("state", state);
    row.put("total", total);
    return row;
  }

  private PreparedStatement prepare(RenderedStatement statement) throws SQLException {
    PreparedStatement prepared = connection.prepareStatement(statement.text());
    for (int i = 0; i < statement.params().size(); i++) {
      prepared.setObject(i + 1, statement.params().get(i));
    }
    return prepared;
  }
}
