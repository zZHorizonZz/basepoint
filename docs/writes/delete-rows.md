# Delete rows

Delete the rows that a filter keeps. A delete needs a filter, or `all()` to delete every row, so that a forgotten
filter does not empty the table.

## Before you begin

The examples on this page use these imports:

```java
import static dev.horizon.basepoint.filter.Filter.*;
```

## Delete rows

1. Start a delete of the table:

   ```java
   DeleteQuery cancel = orders.delete();
   ```

2. Choose the rows with `where`:

   ```java
   cancel.where(eq("state", "CANCELLED"));
   ```

The delete renders:

```sql
DELETE FROM "orders" WHERE ("orders"."state" = $1)
```

## Delete the rows without related rows

Pass `notExists` with a query of the related table:

```java
orders.delete().where(notExists(items.query().on("order_id", "id")));
```

This renders:

```sql
DELETE FROM "orders" WHERE (NOT EXISTS (SELECT 1 FROM "items" AS "t0" WHERE ("t0"."order_id" = "orders"."id")))
```

## Delete every row

Call `all()` instead of `where`:

```java
orders.delete().all();
```

This renders:

```sql
DELETE FROM "orders"
```

## Return the deleted rows

Call `returning` with column names, or with nothing to return all columns. The rows are as they were before the
delete. PostgreSQL, MariaDB and SQLite render `RETURNING`, and SQL Server `OUTPUT DELETED`:

```java
orders.delete().where(eq("id", 1)).returning("id");
```

## Related

- [Write reference](write-reference.md)
- [Filter rows](../filters/filter-rows.md)
- [Troubleshooting writes](troubleshooting-writes.md)
