package dev.horizon.basepoint.sqlserver;

import static dev.horizon.basepoint.Expression.key;
import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.basepoint.Buildable;
import dev.horizon.basepoint.CollectionReference;
import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.LockMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SqlServerDialectTest {

  private static final Dialect DIALECT = new SqlServerDialect();
  private static final CollectionReference ORDERS = DIALECT.collection("orders");
  private static final CollectionReference ITEMS = DIALECT.collection("items");

  static Stream<Arguments> statements() {
    return Stream.of(
        arguments("json key", ORDERS.query().where(eq(key("labels", "env"), "prod")),
            "SELECT [t0].* FROM [orders] AS [t0] WHERE (JSON_VALUE([t0].[labels], @p1) = @p2)"),
        arguments("brackets and at-p placeholders", ORDERS.query().where(eq("state", "OPEN")),
            "SELECT [t0].* FROM [orders] AS [t0] WHERE ([t0].[state] = @p1)"),
        arguments("brackets in names", DIALECT.collection("a]b").query(),
            "SELECT [t0].* FROM [a]]b] AS [t0]"),
        arguments("update lock", ORDERS.query().where(eq("state", "OPEN")).lock(LockMode.UPDATE),
            "SELECT [t0].* FROM [orders] AS [t0] WITH (UPDLOCK, ROWLOCK) WHERE ([t0].[state] = @p1)"),
        arguments("skip locked", ORDERS.query().limit(1).lock(LockMode.UPDATE_SKIP_LOCKED),
            "SELECT TOP (1) [t0].* FROM [orders] AS [t0] WITH (UPDLOCK, ROWLOCK, READPAST)"),
        arguments("limit", ORDERS.query().limit(10),
            "SELECT TOP (10) [t0].* FROM [orders] AS [t0]"),
        arguments("distinct limit", ORDERS.query().columns("state").distinct().limit(10),
            "SELECT DISTINCT TOP (10) [t0].[state] FROM [orders] AS [t0]"),
        arguments("offset", ORDERS.query().orderBy("id").offset(5).limit(10),
            "SELECT [t0].* FROM [orders] AS [t0] ORDER BY [t0].[id] ASC OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY"),
        arguments("offset without order", ORDERS.query().offset(5),
            "SELECT [t0].* FROM [orders] AS [t0] ORDER BY (SELECT NULL) ASC OFFSET 5 ROWS"),
        arguments("insert output", ORDERS.insert(row()).returning("id"),
            "INSERT INTO [orders] ([id], [state]) OUTPUT INSERTED.[id] VALUES (@p1, @p2)"),
        arguments("update output", ORDERS.update().set("state", "CLOSED").where(eq("id", 1)).returning(),
            "UPDATE [orders] SET [state] = @p1 OUTPUT INSERTED.* WHERE ([orders].[id] = @p2)"),
        arguments("delete output", ORDERS.delete().where(eq("id", 1)).returning("id"),
            "DELETE FROM [orders] OUTPUT DELETED.[id] WHERE ([orders].[id] = @p1)"),
        arguments("merge", ORDERS.insert(row()).replace("id").returning("id"),
            "MERGE INTO [orders] WITH (HOLDLOCK) AS [t0] USING (VALUES (@p1, @p2)) AS [t1] ([id], [state])"
                + " ON [t0].[id] = [t1].[id] WHEN MATCHED THEN UPDATE SET [state] = [t1].[state]"
                + " WHEN NOT MATCHED THEN INSERT ([id], [state]) VALUES ([t1].[id], [t1].[state])"
                + " OUTPUT INSERTED.[id];"),
        arguments("merge ignore", ORDERS.insert(row()).ignore("id"),
            "MERGE INTO [orders] WITH (HOLDLOCK) AS [t0] USING (VALUES (@p1, @p2)) AS [t1] ([id], [state])"
                + " ON [t0].[id] = [t1].[id] WHEN NOT MATCHED THEN INSERT ([id], [state])"
                + " VALUES ([t1].[id], [t1].[state]);"),
        arguments("include", ORDERS.query().columns("id").include("items",
            ITEMS.query().on("order_id", "id").columns("sku").orderBy("sku")),
            "SELECT [t0].[id], JSON_QUERY(COALESCE((SELECT [t1].[sku] FROM [items] AS [t1] WHERE"
                + " ([t1].[order_id] = [t0].[id]) ORDER BY [t1].[sku] ASC FOR JSON PATH, INCLUDE_NULL_VALUES), '[]'))"
                + " AS [items] FROM [orders] AS [t0]"),
        arguments("include one", ORDERS.query().columns("id").includeOne("item",
            ITEMS.query().on("order_id", "id").columns("sku")),
            "SELECT [t0].[id], JSON_QUERY((SELECT TOP (1) [t1].[sku] FROM [items] AS [t1] WHERE"
                + " ([t1].[order_id] = [t0].[id]) FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER))"
                + " AS [item] FROM [orders] AS [t0]"),
        arguments("sorted union", ORDERS.query().columns("id").union(ITEMS.query().columns("id")).orderBy("id")
            .limit(3),
            "SELECT TOP (3) [t0].* FROM (SELECT [t1].[id] FROM [orders] AS [t1] UNION ALL SELECT [t2].[id]"
                + " FROM [items] AS [t2]) AS [t0] ORDER BY [t0].[id] ASC"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statements")
  void renders(String name, Buildable builder, String text) {
    assertThat(builder.build().text()).isEqualTo(text);
  }

  private static Map<String, Object> row() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 1);
    row.put("state", "OPEN");
    return row;
  }
}
