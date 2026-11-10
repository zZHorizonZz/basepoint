# Use WITH queries

Name the rows of a query, so that the statement reads them like a table: `WITH big AS (SELECT ...) SELECT ... FROM
big`. Use a WITH query to read the same rows more than once, or to walk a tree with a recursive WITH query, such as
the categories under a category.

## Before you begin

Call `with` on the outermost query. SQL Server allows `WITH` only at the start of a statement, so a query that is
part of another statement, such as an include, a subquery or an operand of a union, cannot have its own WITH
queries.

## Read the rows of a query by a name

1. Write the query of the rows. Leave out `on`, since a WITH query has no enclosing row:

   ```java
   Query big = orders.query().columns("id", "total").where(gt("total", 100));
   ```

2. Start a query of the name with `dialect.collection(name)`, and pass the name and the rows to `with`:

   ```java
   dialect.collection("big").query().with("big", big).columns("id");
   ```

The query renders:

```sql
WITH "big" AS (SELECT "t1"."id", "t1"."total" FROM "orders" AS "t1" WHERE ("t1"."total" > $1))
SELECT "t0"."id" FROM "big" AS "t0"
```

A WITH query can read the WITH queries before it, and the queries in the statement can read it too, such as the
query of an include or of `exists`. The rows keep their sort order only with a limit or an offset.

## Name the rows of a union

The rows can be a union, and a union can have WITH queries:

```java
dialect.collection("all_ids").query()
    .with("all_ids", orders.query().columns("id").union(archived.query().columns("id")))
    .count();
```

This renders:

```sql
WITH "all_ids" AS (SELECT "t1"."id" FROM "orders" AS "t1" UNION ALL SELECT "t2"."id" FROM "archived" AS "t2")
SELECT COUNT(*) AS "count" FROM "all_ids" AS "t0"
```

## Walk a tree

`withRecursive(name, start, step)` renders `WITH RECURSIVE name AS (start UNION ALL step)`. The start gives the
first rows. The step reads the rows so far through a relation to the WITH query, and gives the rows that follow
from them, such as their children:

```java
Query start = categories.query().columns("id", "parent_id").select("depth", value(0)).where(eq("id", 1));
Query step = categories.query()
    .relation("parent", dialect.collection("tree").query().on("id", "parent_id"))
    .columns("id", "parent_id")
    .select("depth", add(column("parent", "depth"), value(1)));

dialect.collection("tree").query().withRecursive("tree", start, step).columns("id", "depth");
```

This renders:

```sql
WITH RECURSIVE "tree" AS (SELECT "t1"."id", "t1"."parent_id", $1 AS "depth" FROM "categories" AS "t1"
WHERE ("t1"."id" = $2) UNION ALL SELECT "t2"."id", "t2"."parent_id", ("t3"."depth" + $3) AS "depth"
FROM "categories" AS "t2" JOIN "tree" AS "t3" ON ("t3"."id" = "t2"."parent_id"))
SELECT "t0"."id", "t0"."depth" FROM "tree" AS "t0"
```

The start and the step select the same columns, in the same order. The relation of the step to the WITH query is an
inner join, since the databases allow the recursive reference only there. To walk up a tree instead, relate the
step to the WITH query by the other column: `on("parent_id", "id")`.

The step limits the depth, such as with `where(lt(column("parent", "depth"), 10))`. A cycle in the rows recurses
until the database stops it: SQL Server stops after 100 levels, and the others run until they run out of memory or
time. SQL Server writes `WITH` without `RECURSIVE`.

## Related

- [Query reference](query-reference.md#query)
- [Troubleshooting queries](troubleshooting-queries.md)
- [Group and combine rows](group-and-combine-rows.md)
