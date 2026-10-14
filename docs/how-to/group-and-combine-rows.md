# Group and combine rows

This page shows how to aggregate rows, remove duplicates and combine the results of two queries.

## Aggregate rows

1. Call `columns` with the grouping columns.
2. Call `select` once for each aggregate. Use `count`, `sum`, `avg`, `min` or `max` from `Expression`.
3. Call `groupBy` with the grouping columns.
4. Optional: call `having` to filter groups.

```java
import static dev.horizon.basepoint.Expression.sum;
import static dev.horizon.basepoint.Filter.gt;

RenderedStatement statement = orders.query()
    .columns("state")
    .select("revenue", sum("total"))
    .groupBy("state")
    .having(gt(sum("total"), 1000))
    .orderBy("revenue", Direction.DESC)
    .build();
```

This renders:

```sql
SELECT "t0"."state", SUM("t0"."total") AS "revenue" FROM "orders" AS "t0" GROUP BY "t0"."state"
HAVING (SUM("t0"."total") > $1) ORDER BY "revenue" DESC
```

`orderBy("revenue")` sorts by the `select` column. Any other name sorts by a table column.

The result type of `AVG` depends on the database. For example, SQL Server returns an integer for an integer column.

## Count rows

`count()` selects `COUNT(*)` as a column named `count`:

```java
orders.query().where(eq("state", "OPEN")).count();
```

## Remove duplicates

`distinct()` renders `SELECT DISTINCT`:

```java
orders.query().columns("state").distinct();
```

## Combine queries

The two queries must return the same columns.

| Method                    | SQL         | Duplicates |
| ------------------------- | ----------- | ---------- |
| `union(other)`            | `UNION ALL` | Kept       |
| `union(other).distinct()` | `UNION`     | Removed    |
| `intersect(other)`        | `INTERSECT` | Removed    |
| `except(other)`           | `EXCEPT`    | Removed    |

```java
RenderedStatement statement = orders.query().columns("id")
    .union(archivedOrders.query().columns("id"))
    .distinct()
    .orderBy("id")
    .build();
```

This renders:

```sql
SELECT "t0".* FROM (SELECT "t1"."id" FROM "orders" AS "t1" UNION SELECT "t2"."id" FROM "archived_orders" AS "t2")
AS "t0" ORDER BY "t0"."id" ASC
```

Operators apply from left to right, in the order of your calls. `a.union(b).except(c)` removes the rows of `c` from
the union of `a` and `b`.

`orderBy`, `limit` and `offset` on a compound query apply to the combined rows. A query that has its own `limit` or
`offset` keeps them inside the compound query.

## Related pages

- [Builders](../reference/builders.md)
