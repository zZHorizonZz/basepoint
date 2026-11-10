# Basepoint

Basepoint is a Java library that builds SQL statements for PostgreSQL, MariaDB, SQL Server and SQLite. You describe a
query, insert, update or delete with a fluent API. Basepoint returns the SQL text and the values for its
placeholders. You run the statement with any database client, such as JDBC, R2DBC or the Vert.x SQL client.
Basepoint does not connect to databases and does not need a schema.

## Get started

To add Basepoint to a project and build your first query, see [Get started](docs/get-started.md). The
[docs](docs/README.md) cover queries, filters, writes and dialects.

## Modules

| Module                        | Package                            | Contents                                                     |
| ----------------------------- | ---------------------------------- | ------------------------------------------------------------ |
| `basepoint-dialect`           | `dev.horizon.basepoint`            | `Dialect`, `CollectionReference`, the builders, the exceptions |
|                               | `dev.horizon.basepoint.expression` | `Expression` and its values                                  |
|                               | `dev.horizon.basepoint.filter`     | `Filter`, its filters and filter text                        |
|                               | `dev.horizon.basepoint.statement`  | The statement tree, `StatementWriter` and `Decorator`        |
| `basepoint-dialect-postgres`  | `dev.horizon.basepoint.postgres`   | `PostgresDialect`                                            |
| `basepoint-dialect-mariadb`   | `dev.horizon.basepoint.mariadb`    | `MariaDbDialect`                                             |
| `basepoint-dialect-sqlserver` | `dev.horizon.basepoint.sqlserver`  | `SqlServerDialect`                                           |
| `basepoint-dialect-sqlite`    | `dev.horizon.basepoint.sqlite`     | `SqliteDialect`                                              |

Each dialect module depends on `basepoint-dialect`. Add only the dialect modules that you use.

## Run the tests

The tests of `basepoint-dialect` that need no database check the statement tree, the decorators, every refusal
of the builders, and the errors of filter text. The contract tests run on the four databases: each dialect module
runs the suites that `DialectContract` lists, from the test JAR of `basepoint-dialect`, against its database.

| Part of the contract tests              | Where it is                                          |
| --------------------------------------- | ---------------------------------------------------- |
| The tables and rows that each suite reads | `basepoint-dialect/src/test/resources/fixture.sql`  |
| Cases of filter text and their rows     | `basepoint-dialect/src/test/resources/filters/`      |
| The suites, one for each kind of statement or input | `basepoint-dialect/src/test/java/.../contract/` |

Each suite loads the fixture once, and each test runs in a transaction that is rolled back, so tests do not see the
writes of other tests. The tests of `basepoint-dialect-postgres` also check the docs: each link must lead to a page
and heading that exist, each Java example must compile, and an example followed by SQL must render that SQL.

Testcontainers starts PostgreSQL, MariaDB and SQL Server in Docker, and SQLite runs in process. Start Docker, then
run:

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
