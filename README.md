# Basepoint

A lightweight SQL builder in the style of a collection client: filters, includes, aggregates and writes in, SQL and
its values out. Basepoint knows no schema and runs nothing; any client runs its statements, such as JDBC, R2DBC or
Vert.x. It writes the SQL of PostgreSQL, MySQL (8.0.31 and later), SQL Server and SQLite (3.44 and later).

```java
Dialect pg = new PostgresDialect();            // $1 placeholders; new PostgresDialect(ParameterStyle.QUESTION_MARK) for JDBC
CollectionReference orders = pg.collection("orders");
CollectionReference items = pg.collection("items");

Statement open = orders.query()
    .where(and(eq("state", "OPEN"), gt("total", 100)))      // or .where("state = \"OPEN\" AND total > 100")
    .include("items", items.query().on("order_id", "id").columns("id", "sku").orderBy("sku").limit(10))
    .orderBy("created", Direction.DESC)
    .limit(20)
    .build();

open.text();    // SELECT "t0".*, (SELECT COALESCE(jsonb_agg(...) ...) AS "items" FROM "orders" AS "t0" WHERE ...
open.params();  // [OPEN, 100]
```

## Modules

| Module | Package | Has |
|---|---|---|
| `basepoint-dialect` | `dev.horizon.basepoint` | the builders, filters, expressions, and the parts of SQL that a dialect writes |
| `basepoint-dialect-postgres` | `dev.horizon.basepoint.postgres` | `PostgresDialect` |
| `basepoint-dialect-mysql` | `dev.horizon.basepoint.mysql` | `MySqlDialect` |
| `basepoint-dialect-sqlserver` | `dev.horizon.basepoint.sqlserver` | `SqlServerDialect` |
| `basepoint-dialect-sqlite` | `dev.horizon.basepoint.sqlite` | `SqliteDialect` |

The implementations of the builders are in `dev.horizon.basepoint.impl`, which the module does not export.

## Reads

- `where(Filter)`: `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `in`, `nin`, `between`, `isNull`, `isNotNull`, `startsWith`,
  `endsWith`, `contains`, `like`, `and`, `or`, `not`, `exists`, `notExists`. A value is always bound. `eq(column, null)`
  is `IS NULL`.
- `where(String)`: the EBNF grammar of AIP-160, through aquery; `Filter.of(text, new CelFilterParser())` reads CEL.
- `columns`, `select(name, expression)` with `count`, `sum`, `avg`, `min`, `max`, `groupBy`, `having`, `distinct`,
  `count()`.
- `orderBy`, `limit`, `offset`.
- `include` (a JSON array), `includeOne` (a JSON object), `includeCount`: the related rows of another query, which
  `on(column, outerColumn)` relates. MySQL and SQLite make JSON of the columns that the related query names.
- `union` (keeps duplicates; `distinct()` removes them), `intersect`, `except`, from left to right as the calls read.

## Writes

- `insert(row)`, `insert(rows)`, with `ignore(key...)` or `replace(key...)` for upserts.
- `update().set(...).increment(...).unset(...)`, `delete()`: each needs `where` or `all()`.
- `returning(columns...)`: `RETURNING`, or `OUTPUT` on SQL Server. MySQL has none and refuses it.

## Tests

`mvn verify` runs the same contract tests against each database, in containers of Testcontainers.
