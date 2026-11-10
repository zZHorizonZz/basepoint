# Group and combine rows

Aggregate the rows of a table by groups, such as the revenue of each state, or combine the rows of several queries
with `union`, `intersect` and `except`. For one aggregate of related rows in each row, use a
[subquery](include-related-rows.md#count-related-rows) instead.

## Before you begin

The examples on this page use these imports:

```java
import static dev.horizon.basepoint.expression.Expression.*;
import static dev.horizon.basepoint.filter.Filter.*;
```

## Aggregate rows

1. Select the grouping columns with `columns`:

   ```java
   Query byState = orders.query().columns("state");
   ```

2. Select each aggregate under a name, with `count`, `sum`, `avg`, `min` or `max` from `Expression`:

   ```java
   byState = byState.select("revenue", sum("total"));
   ```

3. Group by the grouping columns:

   ```java
   byState = byState.groupBy("state");
   ```

4. Optional: filter the groups with `having`, and sort them:

   ```java
   byState.having(gt(sum("total"), 1000)).orderBy("revenue", Direction.DESC);
   ```

The query renders:

```sql
SELECT "t0"."state", SUM("t0"."total") AS "revenue" FROM "orders" AS "t0" GROUP BY "t0"."state"
HAVING (SUM("t0"."total") > $1) ORDER BY "revenue" DESC
```

`orderBy("revenue")` sorts by the `select` column of that name. Any other name sorts by a table column.

The result type of `AVG` depends on the database. For example, SQL Server returns an integer for an integer column.

## Count rows

`count()` selects `COUNT(*)` as a column named `count`:

```java
orders.query().where(eq("state", "OPEN")).count();
```

This renders:

```sql
SELECT COUNT(*) AS "count" FROM "orders" AS "t0" WHERE ("t0"."state" = $1)
```

## Remove duplicates

`distinct()` renders `SELECT DISTINCT`:

```java
orders.query().columns("state").distinct();
```

## Combine queries

`union`, `intersect` and `except` return a query of the combined rows. The queries must select the same columns.

| Method                   | SQL         | Duplicates |
| ------------------------ | ----------- | ---------- |
| `union(rows)`            | `UNION ALL` | Kept       |
| `union(rows).distinct()` | `UNION`     | Removed    |
| `intersect(rows)`        | `INTERSECT` | Removed    |
| `except(rows)`           | `EXCEPT`    | Removed    |

```java
orders.query().columns("id")
    .union(archived.query().columns("id"))
    .distinct()
    .orderBy("id");
```

This renders:

```sql
SELECT "t0".* FROM (SELECT "t1"."id" FROM "orders" AS "t1" UNION SELECT "t2"."id" FROM "archived" AS "t2")
AS "t0" ORDER BY "t0"."id" ASC
```

Operators apply from left to right, in the order of your calls. `a.union(b).except(c)` removes the rows of `c` from
the union of `a` and `b`.

The combined rows are a query like any other: `where`, `orderBy`, `limit`, `offset`, `count` and `with` apply to the
combined rows, and you can pass them to `exists` or to `with` of another query. A query that has its own `limit` or
`offset` keeps them inside the combination. The queries must come from equal dialects; see
[Troubleshooting queries](troubleshooting-queries.md).

## Related

- [Query reference](query-reference.md)
- [Use WITH queries](use-with-queries.md)
