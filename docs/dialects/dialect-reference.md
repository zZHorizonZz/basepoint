# Dialect reference

The dialects, their quotes and placeholders, and the SQL that differs between the databases. Each dialect is in its
own module. For how a dialect renders, see [About statements and decorators](about-statements-and-decorators.md).

## Quick reference

| Dialect            | Module                        | Minimum version | Identifier quotes | Default placeholder |
| ------------------ | ----------------------------- | --------------- | ----------------- | ------------------- |
| `PostgresDialect`  | `basepoint-dialect-postgres`  | PostgreSQL 13   | `"name"`          | `$1`                |
| `MariaDbDialect`   | `basepoint-dialect-mariadb`   | MariaDB 11.4    | `` `name` ``      | `?`                 |
| `SqlServerDialect` | `basepoint-dialect-sqlserver` | SQL Server 2017 | `[name]`          | `@p1`               |
| `SqliteDialect`    | `basepoint-dialect-sqlite`    | SQLite 3.44     | `"name"`          | `?`                 |

A quote character in a name is doubled, and a `]` on SQL Server. Each dialect has a no-argument constructor that
uses the default placeholder. To use another placeholder style, pass a `ParameterStyle`:

| Client                                    | Placeholder style              |
| ----------------------------------------- | ------------------------------ |
| JDBC, all databases                       | `ParameterStyle.QUESTION_MARK` |
| Vert.x PostgreSQL client                  | `ParameterStyle.DOLLAR`        |
| Vert.x MySQL client, connected to MariaDB | `ParameterStyle.QUESTION_MARK` |
| Vert.x SQL Server client                  | `ParameterStyle.AT_P`          |

```java
Dialect dialect = new SqlServerDialect(ParameterStyle.QUESTION_MARK);
```

Each dialect class has a `NAME` constant with the database name, for example `MariaDbDialect.NAME`.

## Differences by database

| Feature                     | PostgreSQL                   | MariaDB                                               | SQL Server                             | SQLite                                              |
| --------------------------- | ---------------------------- | ----------------------------------------------------- | -------------------------------------- | --------------------------------------------------- |
| Limit                       | `LIMIT n`                    | `LIMIT n`                                             | `SELECT TOP (n)`                       | `LIMIT n`                                           |
| Offset                      | `OFFSET m`                   | `LIMIT 18446744073709551615 OFFSET m` without a limit | `OFFSET m ROWS FETCH NEXT n ROWS ONLY` | `LIMIT -1 OFFSET m` without a limit                 |
| Offset without `orderBy`    | Allowed                      | Allowed                                               | Adds `ORDER BY (SELECT NULL)`          | Allowed                                             |
| Returned rows               | `RETURNING`                  | `RETURNING`                                           | `OUTPUT INSERTED` or `OUTPUT DELETED`  | `RETURNING`                                         |
| Returned rows of an update  | Supported                    | MariaDB 13.0 and later                                | Supported                              | Supported                                           |
| Upsert                      | `ON CONFLICT`                | `ON DUPLICATE KEY UPDATE`                             | `MERGE ... WITH (HOLDLOCK)`            | `ON CONFLICT`                                       |
| Upsert key                  | The columns you pass         | Any unique key of the table                           | The columns you pass                   | The columns you pass                                |
| Default row                 | `DEFAULT VALUES`             | `() VALUES ()`                                        | `DEFAULT VALUES`                       | `DEFAULT VALUES`                                    |
| Include function            | `jsonb_agg`, `to_jsonb`      | `JSON_ARRAYAGG`, `JSON_OBJECT`                        | `FOR JSON PATH`                        | `json_group_array`, `json_object`                   |
| Include without `columns`   | Supported                    | Not supported                                         | Supported                              | Not supported                                       |
| Include size limit          | None                         | `group_concat_max_len`, 1 MB by default               | None                                   | None                                                |
| Value of a JSON key         | `column ->> CAST(? AS text)` | `JSON_VALUE(column, ?)`                               | `JSON_VALUE(column, ?)`                | `CAST(json_extract(column, ?) AS TEXT)`             |
| Relation                    | `LEFT JOIN`                  | `LEFT JOIN`                                           | `LEFT JOIN`                            | `LEFT JOIN`                                         |
| Recursive WITH query        | `WITH RECURSIVE`             | `WITH RECURSIVE`                                      | `WITH`, at most 100 levels             | `WITH RECURSIVE`                                    |
| `concat`                    | `\|\|`                       | `CONCAT`                                              | `+`                                    | `\|\|`                                              |
| `length`                    | `CHAR_LENGTH`                | `CHAR_LENGTH`                                         | `LEN(text + 'x') - 1`                  | `length`                                            |
| `now()`                     | `CURRENT_TIMESTAMP`          | `UTC_TIMESTAMP(6)`                                    | `SYSUTCDATETIME()`                     | `strftime(...)`                                     |
| A timestamp moved           | `INTERVAL '... seconds'`     | `INTERVAL ... MICROSECOND`                            | `DATEADD`, at most 68 years            | `strftime`, to the millisecond                      |
| `INTERSECT`, `EXCEPT`       | Supported                    | Supported                                             | Supported                              | Supported                                           |
| Share lock                  | `FOR SHARE`                  | `LOCK IN SHARE MODE`                                  | `WITH (HOLDLOCK, ROWLOCK)`             | None. A transaction that writes locks the database. |
| Update lock                 | `FOR UPDATE`                 | `FOR UPDATE`                                          | `WITH (UPDLOCK, ROWLOCK)`              | None                                                |
| Lock that skips locked rows | `FOR UPDATE SKIP LOCKED`     | `FOR UPDATE SKIP LOCKED`                              | `WITH (UPDLOCK, ROWLOCK, READPAST)`    | None                                                |
| Lock that does not wait     | `FOR UPDATE NOWAIT`          | `FOR UPDATE NOWAIT`                                   | `WITH (UPDLOCK, ROWLOCK, NOWAIT)`      | None                                                |
| Text without case           | By collation                 | By default                                            | By default                             | `LIKE` of ASCII letters                             |

When a database cannot render a statement, `build()` throws `UnsupportedFeatureException`. MariaDB 11.4 is an
exception: it rejects `UPDATE ... RETURNING` when you run the statement.

## Decorators

Each dialect returns its decorators from `decorator()`. The decorators run in this order.

| Dialect            | Decorators                                                                                                                                                                         |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PostgresDialect`  | `Include.One` to `PostgresObject`, `Include.Many` to `PostgresArray`, `JsonKey` to `PostgresJsonKey`                                                                               |
| `MariaDbDialect`   | `MariaDbLimits`, `Concat`, `Now`, `Shift`, `OnConflict` to `MariaDbOnDuplicateKey`, `DefaultValues` to `MariaDbEmptyRow`, `Include.One` to `MariaDbObject`, `Include.Many` to `MariaDbArray`, `Lock` to `MariaDbLock` |
| `SqlServerDialect` | `SqlServerLock`, `Concat`, `Length`, `Now`, `Shift` to `SqlServerShift`, `With`, `SqlServerMerge`, `SqlServerOutput`, `SqlServerPaging`, `Include.One` to `SqlServerObject`, `Include.Many` to `SqlServerArray` |
| `SqliteDialect`    | `SqliteLimits`, `Length`, `Now`, `Shift`, `Include.One` to `SqliteObject`, `Include.Many` to `SqliteArray`, `JsonKey` to `SqliteJsonKey`, removal of `Lock`                        |

To add your own decorator, see [Add a decorator](add-a-decorator.md).

## Generated names

Basepoint generates these names in the SQL:

| Name            | Use                                                                                                                     |
| --------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `t0`, `t1`, ... | Aliases of tables and subqueries, unique within one statement                                                           |
| `bp_n`          | Row number of included rows on PostgreSQL and SQLite, used to keep their sort order. It is not part of the JSON result. |

A sorted include of a query that selects a column named `bp_n` throws `InvalidQueryException`.

## Related

- [Troubleshooting dialects](troubleshooting-dialects.md)
- [Query reference](../queries/query-reference.md)
