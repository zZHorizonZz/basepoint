# Troubleshooting dialects

The errors and surprises of each database, the most frequent first.

## A JSON object of related rows needs their column names. List the...

```text
dev.horizon.basepoint.UnsupportedFeatureException: A JSON object of related rows needs their column names. List the columns of the included query with columns().
```

**Cause:** An include of all columns on MariaDB or SQLite. These databases build a JSON object from named columns,
and Basepoint does not know the columns of a table.

**Fix:** Call `columns` on the included query, such as `items.query().on("order_id", "id").columns("id", "sku")`.

**Verify:** The SQL has `JSON_OBJECT('id', ...)` on MariaDB, or `json_object('id', ...)` on SQLite.

## contains("body", "🦀") matches every row on SQL Server

```text
SELECT [t0].[id] FROM [texts] AS [t0] WHERE ([t0].[body] LIKE @p1 ESCAPE '!')
```

**Cause:** The column has a collation without supplementary characters, such as the default
`SQL_Latin1_General_CP1_CI_AS`. It does not compare characters beyond the Basic Multilingual Plane, such as emoji,
so the pattern `%🦀%` matches every text. MariaDB compares them only in a column of `utf8mb4`, and a `_general_ci`
collation of it treats them all as equal.

**Fix:** Give the column a collation with supplementary characters, such as
`NVARCHAR(MAX) COLLATE Latin1_General_100_CI_AS_SC` on SQL Server, or `utf8mb4_unicode_ci` on MariaDB.

**Verify:** The filter matches only the rows whose text has the emoji.

## The array of an include ends early on MariaDB

```sql
SET SESSION group_concat_max_len = 16777216;
```

**Cause:** MariaDB builds an included array with `JSON_ARRAYAGG`, whose result is at most `group_concat_max_len`
bytes, which is 1048576 (1 MB) by default. MariaDB cuts a longer result.

**Fix:** Raise the limit for the session before you run the query, as the statement above does, or limit the
included rows with `limit`.

**Verify:** The array has as many rows as the included query gives on its own.

## SQL Server moves a timestamp by at most 68 years, but the duration is...

```text
dev.horizon.basepoint.UnsupportedFeatureException: SQL Server moves a timestamp by at most 68 years, but the duration is PT876000H.
```

**Cause:** `add` or `subtract` of a timestamp and a duration beyond 68 years on SQL Server, whose `DATEADD` takes the
seconds as an `int`.

**Fix:** Move the timestamp in steps of at most 68 years, or compare with a timestamp that Java computes.

**Verify:** The SQL has `DATEADD(second, ...)` with at most 2147483647 seconds.

## The Standard dialect does not render includes.

```text
dev.horizon.basepoint.UnsupportedFeatureException: The Standard dialect does not render includes.
```

**Cause:** An include on a dialect of your own without decorators of `Include.One` and `Include.Many`. Standard SQL
has no syntax for an include.

**Fix:** Decorate `Include.One` and `Include.Many` into the JSON functions of your database. The dialects of
Basepoint show how, such as `PostgresObject` and `PostgresArray`.

**Verify:** The query with an include builds.

## Related

- [Dialect reference](dialect-reference.md)
- [Exceptions](../exceptions.md)
