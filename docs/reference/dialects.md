# Dialects

A dialect renders statements for one database. Each dialect is in its own module.

## Quick reference

| Dialect            | Module                        | Minimum version | Identifier quotes | Default placeholder |
| ------------------ | ----------------------------- | --------------- | ----------------- | ------------------- |
| `PostgresDialect`  | `basepoint-dialect-postgres`  | PostgreSQL 13   | `"name"`          | `$1`                |
| `MariaDbDialect`   | `basepoint-dialect-mariadb`   | MariaDB 11.4    | `` `name` ``      | `?`                 |
| `SqlServerDialect` | `basepoint-dialect-sqlserver` | SQL Server 2017 | `[name]`          | `@p1`               |
| `SqliteDialect`    | `basepoint-dialect-sqlite`    | SQLite 3.44     | `"name"`          | `?`                 |

Each dialect has a no-argument constructor that uses the default placeholder. To use another placeholder style,
pass a `ParameterStyle`:

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

| Feature                    | PostgreSQL              | MariaDB                                               | SQL Server                             | SQLite                              |
| -------------------------- | ----------------------- | ----------------------------------------------------- | -------------------------------------- | ----------------------------------- |
| Limit                      | `LIMIT n`               | `LIMIT n`                                             | `SELECT TOP (n)`                       | `LIMIT n`                           |
| Offset                     | `OFFSET m`              | `LIMIT 18446744073709551615 OFFSET m` without a limit | `OFFSET m ROWS FETCH NEXT n ROWS ONLY` | `LIMIT -1 OFFSET m` without a limit |
| Offset without `orderBy`   | Allowed                 | Allowed                                               | Adds `ORDER BY (SELECT NULL)`          | Allowed                             |
| Returned rows              | `RETURNING`             | `RETURNING`                                           | `OUTPUT INSERTED` or `OUTPUT DELETED`  | `RETURNING`                         |
| Returned rows of an update | Supported               | MariaDB 13.0 and later                                | Supported                              | Supported                           |
| Upsert                     | `ON CONFLICT`           | `ON DUPLICATE KEY UPDATE`                             | `MERGE ... WITH (HOLDLOCK)`            | `ON CONFLICT`                       |
| Upsert key                 | The columns you pass    | Any unique key of the table                           | The columns you pass                   | The columns you pass                |
| Default row                | `DEFAULT VALUES`        | `() VALUES ()`                                        | `DEFAULT VALUES`                       | `DEFAULT VALUES`                    |
| Include function           | `jsonb_agg`, `to_jsonb` | `JSON_ARRAYAGG`, `JSON_OBJECT`                        | `FOR JSON PATH`                        | `json_group_array`, `json_object`   |
| Include without `columns`  | Supported               | Not supported                                         | Supported                              | Not supported                       |
| Include size limit         | None                    | `group_concat_max_len`, 1 MB by default               | None                                   | None                                |
| `INTERSECT`, `EXCEPT`      | Supported               | Supported                                             | Supported                              | Supported                           |

When a feature is not supported, `build()` throws `UnsupportedOperationException`. MariaDB 11.4 is an exception:
it rejects `UPDATE ... RETURNING` when you run the statement.

## Decorators

Each dialect returns its decorators from `decorator()`. The decorators run in this order.

| Dialect            | Decorators                                                                                                                 |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------- |
| `PostgresDialect`  | `Include` to `PostgresJson`                                                                                                |
| `MariaDbDialect`   | `MariaDbLimits`, `OnConflict` to `MariaDbOnDuplicateKey`, `DefaultValues` to `MariaDbEmptyRow`, `Include` to `MariaDbJson` |
| `SqlServerDialect` | `SqlServerMerge`, `SqlServerOutput`, `SqlServerPaging`, `Include` to `SqlServerJson`                                       |
| `SqliteDialect`    | `SqliteLimits`, `Include` to `SqliteJson`                                                                                  |

To add your own decorator, see [Add a decorator](../how-to/add-a-decorator.md).

## Generated names

Basepoint generates these names in the SQL:

| Name            | Use                                                                                                                     |
| --------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `t0`, `t1`, ... | Aliases of tables and subqueries, unique within one statement                                                           |
| `bp_n`          | Row number of included rows on PostgreSQL and SQLite, used to keep their sort order. It is not part of the JSON result. |

Do not use `bp_n` as a column name in a query that you include.
