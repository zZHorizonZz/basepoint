# Write rows

This page shows how to insert, upsert, update and delete rows.

## Insert rows

Pass a map of column values to `insert`. To insert several rows, pass a list of maps. Every row must set the same
columns.

```java
Map<String, Object> row = new LinkedHashMap<>();
row.put("id", 6);
row.put("state", "OPEN");

RenderedStatement statement = orders.insert(row).build();
```

This renders `INSERT INTO "orders" ("id", "state") VALUES ($1, $2)`.

The column order comes from the iteration order of the first map. Use a `LinkedHashMap` if the order matters to you.

To insert a row that uses the default value of every column, pass an empty map:

```java
defaults.insert(Map.of());
```

## Upsert rows

An upsert handles rows whose key already exists in the table.

| Method            | Behavior for an existing key                                       |
| ----------------- | ------------------------------------------------------------------ |
| `ignore(key...)`  | Keeps the existing row and skips the new row.                      |
| `replace(key...)` | Updates the existing row with all inserted columns except the key. |

```java
orders.insert(row).replace("id");
```

Each dialect renders an upsert in its own syntax:

| Database           | SQL                                                                      |
| ------------------ | ------------------------------------------------------------------------ |
| PostgreSQL, SQLite | `INSERT ... ON CONFLICT ("id") DO UPDATE SET "state" = EXCLUDED."state"` |
| MariaDB            | `INSERT ... ON DUPLICATE KEY UPDATE state = VALUES(state)`               |
| SQL Server         | `MERGE INTO [orders] WITH (HOLDLOCK) ...`                                |

MariaDB matches any unique key of the table, not only the columns that you pass.

## Update rows

1. Call `update()`.
2. Set columns with `set`, `increment` or `unset`.
3. Add a filter with `where`. To update every row, call `all()` instead.

```java
orders.update()
    .set("state", "CLOSED")
    .increment("total", 5)
    .where(eq("id", 1));
```

This renders:

```sql
UPDATE "orders" SET "state" = $1, "total" = ("orders"."total" + $2) WHERE ("orders"."id" = $3)
```

If an update has no filter and no `all()`, `build()` throws `IllegalStateException`.

## Delete rows

Call `delete()`, then add a filter with `where`. To delete every row, call `all()` instead.

```java
orders.delete().where(eq("state", "CANCELLED"));
```

If a delete has no filter and no `all()`, `build()` throws `IllegalStateException`.

## Return the written rows

Call `returning` on an insert, update or delete. Pass column names, or pass nothing to return all columns.

```java
orders.update().set("state", "CLOSED").where(eq("id", 1)).returning("id");
```

| Database                    | SQL                                                           |
| --------------------------- | ------------------------------------------------------------- |
| PostgreSQL, MariaDB, SQLite | `RETURNING "id"` at the end of the statement                  |
| SQL Server                  | `OUTPUT INSERTED.[id]`, or `OUTPUT DELETED.[id]` for a delete |

MariaDB supports `RETURNING` on updates from version 13.0. MariaDB 11.4 rejects an update with `returning`. On
MariaDB 11.4, read the updated rows with a SELECT in the same transaction.

## Related pages

- [Builders](../reference/builders.md)
- [Dialects](../reference/dialects.md)
