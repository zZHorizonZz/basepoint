package dev.horizon.basepoint.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.DeleteQuery;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.InsertQuery;
import dev.horizon.basepoint.Query;
import dev.horizon.basepoint.RenderedStatement;
import dev.horizon.basepoint.UpdateQuery;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractTest {

  private final ObjectMapper mapper = new ObjectMapper();

  protected TestDatabase database;
  protected Dialect dialect;
  protected CollectionReference orders;
  protected CollectionReference items;
  protected CollectionReference products;
  protected CollectionReference archived;
  protected CollectionReference defaults;
  protected CollectionReference labelled;
  protected CollectionReference categories;
  private Connection connection;

  protected abstract TestDatabase database();

  @BeforeAll
  void connect() throws SQLException {
    database = database();
    dialect = database.dialect();
    connection = database.connect();
    new Fixture(dialect, database.types()).load(connection);
    connection.setAutoCommit(false);
    orders = dialect.collection("orders");
    items = dialect.collection("items");
    products = dialect.collection("products");
    archived = dialect.collection("archived");
    defaults = dialect.collection("defaults");
    labelled = dialect.collection("labelled");
    categories = dialect.collection("categories");
  }

  @AfterEach
  void rollback() throws SQLException {
    connection.rollback();
  }

  @AfterAll
  void disconnect() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  protected Connection connection() {
    return connection;
  }

  protected List<Integer> ids(Query query) throws SQLException {
    return idsInOrder(query.orderBy("id"));
  }

  protected List<Integer> idsInOrder(Query query) throws SQLException {
    return idsOf(rows(query));
  }

  protected List<Integer> sortedIds(RenderedStatement statement) throws SQLException {
    List<Integer> ids = new ArrayList<>(idsOf(rows(statement)));
    ids.sort(null);
    return ids;
  }

  protected List<Integer> idsOf(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> (int) number(row.get("id"))).toList();
  }

  protected List<Object> valuesOf(String column, List<Map<String, Object>> rows) {
    return rows.stream().map(row -> row.get(column)).toList();
  }

  protected List<Map<String, Object>> rows(Query query) throws SQLException {
    return rows(query.build());
  }

  protected List<Map<String, Object>> rows(InsertQuery insert) throws SQLException {
    return rows(insert.build());
  }

  protected List<Map<String, Object>> rows(UpdateQuery update) throws SQLException {
    return rows(update.build());
  }

  protected List<Map<String, Object>> rows(DeleteQuery delete) throws SQLException {
    return rows(delete.build());
  }

  protected List<Map<String, Object>> rows(RenderedStatement statement) throws SQLException {
    try (PreparedStatement jdbc = prepare(statement)) {
      boolean hasRows = jdbc.execute();
      while (!hasRows && jdbc.getUpdateCount() != -1) {
        hasRows = jdbc.getMoreResults();
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      if (!hasRows) {
        return rows;
      }
      try (ResultSet cursor = jdbc.getResultSet()) {
        ResultSetMetaData meta = cursor.getMetaData();
        while (cursor.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i).toLowerCase(), cursor.getObject(i));
          }
          rows.add(row);
        }
      }
      return rows;
    }
  }

  protected int execute(InsertQuery insert) throws SQLException {
    return execute(insert.build());
  }

  protected int execute(UpdateQuery update) throws SQLException {
    return execute(update.build());
  }

  protected int execute(DeleteQuery delete) throws SQLException {
    return execute(delete.build());
  }

  protected int execute(RenderedStatement statement) throws SQLException {
    try (PreparedStatement jdbc = prepare(statement)) {
      return jdbc.executeUpdate();
    }
  }

  protected long number(Object value) {
    return ((Number) value).longValue();
  }

  protected JsonNode json(Object value) {
    try {
      return mapper.readTree(value.toString());
    } catch (Exception failure) {
      throw new IllegalArgumentException("The value is not JSON: " + value, failure);
    }
  }

  protected Map<String, Object> order(int id, String state, int total) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("state", state);
    row.put("total", total);
    return row;
  }

  private PreparedStatement prepare(RenderedStatement statement) throws SQLException {
    PreparedStatement jdbc = connection.prepareStatement(statement.text());
    for (int i = 0; i < statement.params().size(); i++) {
      jdbc.setObject(i + 1, statement.params().get(i));
    }
    return jdbc;
  }
}
