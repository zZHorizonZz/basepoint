# Update rows

Set, increment and unset the columns of the rows that a filter keeps. An update needs a filter, or `all()` to update
every row, so that a forgotten filter does not change the whole table.

## Before you begin

The examples on this page use these imports:

```java
import static dev.horizon.basepoint.expression.Expression.*;
import static dev.horizon.basepoint.filter.Filter.*;
```

## Update rows

1. Start an update of the table:

   ```java
   UpdateQuery close = orders.update();
   ```

2. Set the columns with `set`, `increment` or `unset`. A later assignment to the same column replaces the earlier
   one:

   ```java
   close = close.set("state", "CLOSED").increment("total", 5);
   ```

3. Choose the rows with `where`:

   ```java
   close.where(eq("id", 1));
   ```

The update renders:

```sql
UPDATE "orders" SET "state" = $1, "total" = ("orders"."total" + $2) WHERE ("orders"."id" = $3)
```

`set(column, null)` and `unset(column)` set the column to `NULL`. A value can be an `Expression` of the row, such as
`multiply(column("total"), value(2))`.

## Update every row

Call `all()` instead of `where`:

```java
orders.update().set("note", "checked").all();
```

This renders:

```sql
UPDATE "orders" SET "note" = $1
```

## Return the updated rows

Call `returning` with column names, or with nothing to return all columns. The rows are as they are after the
update:

```java
orders.update().set("state", "CLOSED").where(eq("id", 1)).returning("id");
```

| Database                    | SQL                                          |
| --------------------------- | -------------------------------------------- |
| PostgreSQL, MariaDB, SQLite | `RETURNING "id"` at the end of the statement |
| SQL Server                  | `OUTPUT INSERTED.[id]` after `SET`           |

MariaDB supports `RETURNING` on updates from version 13.0. See
[Troubleshooting writes](troubleshooting-writes.md) for MariaDB 11.4.

## Related

- [Write reference](write-reference.md)
- [Filter rows](../filters/filter-rows.md)
- [Troubleshooting writes](troubleshooting-writes.md)
