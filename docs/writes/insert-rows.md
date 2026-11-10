# Insert rows

Insert one or several rows into a table, and decide what happens to a row whose key exists. Use an upsert rather than
a SELECT before the insert, so that two writers cannot both insert the same key.

## Insert a row

1. Put the value of each column in a map. A value can be an `Expression`, such as `now()`, or `null`. The column
   order comes from the iteration order of the map, so use a `LinkedHashMap` if the order matters to you:

   ```java
   Map<String, Object> row = new LinkedHashMap<>();
   row.put("id", 6);
   row.put("state", "OPEN");
   row.put("note", null);
   ```

2. Pass the map to `insert`:

   ```java
   orders.insert(row);
   ```

The insert renders:

```sql
INSERT INTO "orders" ("id", "state", "note") VALUES ($1, $2, NULL)
```

A `null` value renders `NULL`.

## Insert several rows

Pass a list of maps to insert them in one statement. Every row must set the same columns:

```java
orders.insert(List.of(Map.of("id", 6), Map.of("id", 7)));
```

This renders:

```sql
INSERT INTO "orders" ("id") VALUES ($1), ($2)
```

## Insert the default values

To insert a row that uses the default value of every column, pass an empty map:

```java
defaults.insert(Map.of());
```

This renders:

```sql
INSERT INTO "defaults" DEFAULT VALUES
```

## Upsert rows

An upsert handles rows whose key already exists in the table.

| Method            | Behavior for an existing key                                       |
| ----------------- | ------------------------------------------------------------------ |
| `ignore(key...)`  | Keeps the existing row and skips the new row.                      |
| `replace(key...)` | Updates the existing row with all inserted columns except the key. |

```java
orders.insert(Map.of("id", 6, "state", "OPEN")).replace("id");
```

Each dialect renders an upsert in its own syntax:

| Database           | SQL                                                                      |
| ------------------ | ------------------------------------------------------------------------ |
| PostgreSQL, SQLite | `INSERT ... ON CONFLICT ("id") DO UPDATE SET "state" = EXCLUDED."state"` |
| MariaDB            | ``INSERT ... ON DUPLICATE KEY UPDATE `state` = VALUES(`state`)``         |
| SQL Server         | `MERGE INTO [orders] WITH (HOLDLOCK) AS [t0] USING (VALUES ...) ...`     |

MariaDB matches any unique key of the table, not only the columns that you pass.

## Return the inserted rows

Call `returning` with column names, or with nothing to return all columns. PostgreSQL, MariaDB and SQLite render
`RETURNING`, and SQL Server `OUTPUT INSERTED`:

```java
orders.insert(Map.of("id", 6, "state", "OPEN")).returning("id");
```

## Related

- [Write reference](write-reference.md)
- [Troubleshooting writes](troubleshooting-writes.md)
