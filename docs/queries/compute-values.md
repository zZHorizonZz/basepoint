# Compute values

Compute a value for each row in the database: a function, a sum, a condition, a moved timestamp, or the value of a
subquery. Select it with `select`, filter by it, sort by it, group by it, or write it with `set` and `insert`. Compute
in the database rather than in Java when you filter, sort or page by the value.

## Before you begin

The examples on this page use these imports:

```java
import static dev.horizon.basepoint.expression.Expression.*;
import static dev.horizon.basepoint.filter.Filter.*;
```

## Select a computed value

1. Build the value from the methods of `Expression`. `column` reads a column, `value` binds a value, and functions
   such as `concat` and `lower` combine them:

   ```java
   Expression label = concat(column("state"), value("-"), lower(column("region")));
   ```

2. Select the value under a name:

   ```java
   orders.query().columns("id").select("label", label);
   ```

The query renders:

```sql
SELECT "t0"."id", ("t0"."state" || $1 || LOWER("t0"."region")) AS "label" FROM "orders" AS "t0"
```

`concat` gives `NULL` when one of its texts is `NULL`. To give a value instead, wrap the text in `coalesce`:
`coalesce(column("region"), value("none"))`.

## Calculate with numbers

`add`, `subtract`, `multiply` and `divide` take two expressions:

```java
orders.query()
    .columns("id")
    .select("with_tax", multiply(column("total"), value(new BigDecimal("1.21"))))
    .where(gt(subtract(column("total"), column("discount")), 100));
```

This renders:

```sql
SELECT "t0"."id", ("t0"."total" * $1) AS "with_tax" FROM "orders" AS "t0"
WHERE (("t0"."total" - "t0"."discount") > $2)
```

With two integers, `divide` gives an integer on PostgreSQL, SQL Server and SQLite, and a decimal on MariaDB. To get
a decimal everywhere, divide by a decimal value.

## Choose a value

`when(condition, then, otherwise)` renders `CASE WHEN`:

```java
orders.query()
    .columns("id")
    .select("size", when(gt("total", 200), value("big"), value("small")));
```

This renders:

```sql
SELECT "t0"."id", CASE WHEN ("t0"."total" > $1) THEN $2 ELSE $3 END AS "size" FROM "orders" AS "t0"
```

## Work with time

`now()` is the current time in UTC. `add(timestamp, duration)` moves a timestamp later by a `java.time.Duration`,
and `subtract(timestamp, duration)` moves it earlier:

```java
events.query().where(gt("happened", subtract(now(), Duration.ofDays(7))));
```

This renders:

```sql
SELECT "t0".* FROM "events" AS "t0"
WHERE ("t0"."happened" > (CURRENT_TIMESTAMP + INTERVAL '-604800.000000 seconds'))
```

A duration keeps its microseconds, and its milliseconds on SQLite. SQL Server moves a timestamp by at most 68 years;
see [Troubleshooting dialects](../dialects/troubleshooting-dialects.md).

## Read a value of related rows

`subquery` turns a query of related rows that gives one value into a value:

```java
orders.query()
    .columns("id")
    .select("largest", subquery(items.query().on("order_id", "id").select("largest", max("qty"))));
```

This renders:

```sql
SELECT "t0"."id", (SELECT MAX("t1"."qty") AS "largest" FROM "items" AS "t1" WHERE ("t1"."order_id" = "t0"."id"))
AS "largest" FROM "orders" AS "t0"
```

For more, see [Include related rows](include-related-rows.md#count-related-rows).

## SQL by database

The same expression renders the SQL of each database:

| Expression      | PostgreSQL                              | MariaDB                                   | SQL Server                                            | SQLite                                     |
| --------------- | --------------------------------------- | ----------------------------------------- | ----------------------------------------------------- | ------------------------------------------ |
| `concat(a, b)`  | `(a \|\| b)`                            | `CONCAT(a, b)`                            | `(a + b)`                                             | `(a \|\| b)`                               |
| `length(text)`  | `CHAR_LENGTH(text)`                     | `CHAR_LENGTH(text)`                       | `(LEN(text + 'x') - 1)`                               | `length(text)`                             |
| `now()`         | `CURRENT_TIMESTAMP`                     | `UTC_TIMESTAMP(6)`                        | `SYSUTCDATETIME()`                                    | `strftime('%Y-%m-%dT%H:%M:%fZ', 'now')`    |
| `add(t, 1 day)` | `(t + INTERVAL '86400.000000 seconds')` | `(t + INTERVAL 86400000000 MICROSECOND)`  | `DATEADD(microsecond, 0, DATEADD(second, 86400, t))`  | `strftime(..., t, '86400.000000 seconds')` |

`length` counts the spaces at the end of a text on every database.

## Related

- [Query reference](query-reference.md#expression)
- [Include related rows](include-related-rows.md)
