# Basepoint

Basepoint is a Java library that builds SQL statements for PostgreSQL, MariaDB, SQL Server and SQLite. You describe a
query, insert, update or delete with a fluent API. Basepoint returns the SQL text and the values for its
placeholders. You run the statement with any database client, such as JDBC, R2DBC or the Vert.x SQL client.
Basepoint does not connect to databases and does not need a schema.

## Requirements

- Java 25 or later.
- One of these databases:

  | Database   | Minimum version |
  | ---------- | --------------- |
  | PostgreSQL | 13              |
  | MariaDB    | 11.4            |
  | SQL Server | 2017            |
  | SQLite     | 3.44            |

## Get started

1. Add the GitHub Packages repository and the module for your database to your `pom.xml`. This example uses
   PostgreSQL:

   <!-- x-release-please-start-version -->
   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/zZHorizonZz/basepoint</url>
     </repository>
   </repositories>

   <dependencies>
     <dependency>
       <groupId>dev.horizon</groupId>
       <artifactId>basepoint-dialect-postgres</artifactId>
       <version>0.2.0</version>
     </dependency>
   </dependencies>
   ```
   <!-- x-release-please-end -->

2. GitHub Packages requires a token, also for public packages. Create a personal access token with the
   `read:packages` scope, and add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>github</id>
         <username>YOUR_GITHUB_USERNAME</username>
         <password>YOUR_TOKEN</password>
       </server>
     </servers>
   </settings>
   ```

3. Create a dialect and a collection reference for a table. JDBC uses `?` placeholders, so pass
   `ParameterStyle.QUESTION_MARK`:

   ```java
   Dialect dialect = new PostgresDialect(ParameterStyle.QUESTION_MARK);
   CollectionReference orders = dialect.collection("orders");
   ```

4. Build a query:

   ```java
   RenderedStatement statement = orders.query()
       .where(and(eq("state", "OPEN"), gt("total", 100)))
       .orderBy("id")
       .limit(20)
       .build();
   ```

   `statement.text()` returns this SQL:

   ```sql
   SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."state" = ?) AND ("t0"."total" > ?)) ORDER BY "t0"."id" ASC LIMIT 20
   ```

   `statement.params()` returns `[OPEN, 100]`.

5. Run the statement with your database client:

   ```java
   try (PreparedStatement prepared = connection.prepareStatement(statement.text())) {
     for (int i = 0; i < statement.params().size(); i++) {
       prepared.setObject(i + 1, statement.params().get(i));
     }
     try (ResultSet rows = prepared.executeQuery()) {
       while (rows.next()) {
         System.out.println(rows.getInt("id"));
       }
     }
   }
   ```

## Modules

| Module                        | Package                           | Contents                                                          |
| ----------------------------- | --------------------------------- | ----------------------------------------------------------------- |
| `basepoint-dialect`           | `dev.horizon.basepoint`           | The builders, filters, expressions, statement tree and decorators |
| `basepoint-dialect-postgres`  | `dev.horizon.basepoint.postgres`  | `PostgresDialect`                                                 |
| `basepoint-dialect-mariadb`   | `dev.horizon.basepoint.mariadb`   | `MariaDbDialect`                                                  |
| `basepoint-dialect-sqlserver` | `dev.horizon.basepoint.sqlserver` | `SqlServerDialect`                                                |
| `basepoint-dialect-sqlite`    | `dev.horizon.basepoint.sqlite`    | `SqliteDialect`                                                   |

Each dialect module depends on `basepoint-dialect`. Add only the dialect modules that you use.

## Documentation

- Concept: [Statements and decorators](docs/concepts/statements-and-decorators.md)
- How to: [Filter rows](docs/how-to/filter-rows.md)
- How to: [Include related rows](docs/how-to/include-related-rows.md)
- How to: [Group and combine rows](docs/how-to/group-and-combine-rows.md)
- How to: [Write rows](docs/how-to/write-rows.md)
- How to: [Add a decorator](docs/how-to/add-a-decorator.md)
- Reference: [Builders](docs/reference/builders.md)
- Reference: [Filters](docs/reference/filters.md)
- Reference: [Filter text syntax](docs/reference/filter-text-syntax.md)
- Reference: [Dialects](docs/reference/dialects.md)

## Run the tests

Each dialect module runs the contract tests of `basepoint-dialect` against its database. The contract tests are in
the test JAR of `basepoint-dialect`. Testcontainers starts PostgreSQL, MariaDB and SQL Server in Docker, and SQLite
runs in process. Start Docker, then run:

```sh
mvn verify
```

To test another server version, set its image with a system property:

| Property                         | Default                                      |
| -------------------------------- | -------------------------------------------- |
| `basepoint.test.postgres.image`  | `postgres:17-alpine`                         |
| `basepoint.test.mariadb.image`   | `mariadb:13.0`                               |
| `basepoint.test.sqlserver.image` | `mcr.microsoft.com/mssql/server:2022-latest` |

For example: `mvn verify -Dbasepoint.test.mariadb.image=mariadb:11.4`.

## Planned

- **Options of one dialect.** A way to ask for SQL that only one database has, such as PostgreSQL's `@>` for the
  value of a JSON key, which a GIN index answers. Today `Expression.key` renders `->>` on PostgreSQL, and no index
  answers that on any database. The GIN index itself would come from [Leadseal](https://github.com/zZHorizonZz/leadseal),
  which cannot declare an index method yet.

## License

Apache License 2.0. See [LICENSE](LICENSE).
