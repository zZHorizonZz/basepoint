# Troubleshooting writes

The errors of inserts, updates and deletes, the most frequent first.

## The update has no filter. Add a filter with where(), or call all() to...

```text
dev.horizon.basepoint.UnfilteredWriteException: The update has no filter. Add a filter with where(), or call all() to update every row.
```

**Cause:** `build()` or `statement()` of an update without `where` and without `all()`. Basepoint refuses it, so that
a forgotten filter does not change every row of the table. The delete has the same error: `The delete has no
filter.`

**Fix:** Add the filter of the rows to change with `where`. If you mean every row, call `all()`.

**Verify:** `build()` returns the statement, and its text has a `WHERE` clause, or no `WHERE` clause after `all()`.

## The update sets no columns. Call set(), increment() or unset().

```text
java.lang.IllegalStateException: The update sets no columns. Call set(), increment() or unset().
```

**Cause:** `build()` of an update without assignments, such as an update whose `set` calls are in a branch that did
not run.

**Fix:** Call `set`, `increment` or `unset` at least once, or do not run the update when there is nothing to set.

**Verify:** `build()` returns the statement, and its text has a `SET` clause.

## Row 1 sets the columns [state], but the first row sets [id]. Every...

```text
java.lang.IllegalArgumentException: Row 1 sets the columns [state], but the first row sets [id]. Every row must set the same columns.
```

**Cause:** An insert of several rows whose maps have different keys. One statement inserts the same columns for each
row.

**Fix:** Give every map the same keys. Put `null` for a column that a row does not set, or insert such rows in a
statement of their own.

**Verify:** `insert(rows).build()` returns one statement with a `VALUES` list for each row.

## The insert has no rows.

```text
java.lang.IllegalArgumentException: The insert has no rows.
```

**Cause:** `insert(List.of())`, such as an insert of a list that a filter emptied.

**Fix:** Do not run the insert when the list is empty.

**Verify:** The code inserts only lists with at least one row.

## Only one row can use the default value of every column.

```text
java.lang.IllegalArgumentException: Only one row can use the default value of every column.
```

**Cause:** An insert of several empty maps. `DEFAULT VALUES` inserts one row.

**Fix:** Insert each row of default values in a statement of its own.

**Verify:** `insert(Map.of()).build()` renders `INSERT INTO ... DEFAULT VALUES`.

## The key has no columns.

```text
java.lang.IllegalArgumentException: The key has no columns.
```

**Cause:** `ignore()` or `replace()` without the columns of the key.

**Fix:** Pass the columns of the primary key or unique index, such as `replace("id")`.

**Verify:** The upsert renders `ON CONFLICT ("id")` on PostgreSQL and SQLite.

## A row that uses the default value of every column cannot use a key.

```text
java.lang.IllegalArgumentException: A row that uses the default value of every column cannot use a key.
```

**Cause:** `ignore` or `replace` of an insert of an empty map. A row of default values has no key to compare.

**Fix:** Set the columns of the key in the map, or insert the row without an upsert.

**Verify:** The insert builds, and the key columns are in its column list.

## You have an error in your SQL syntax ... near 'RETURNING ...

```text
java.sql.SQLSyntaxErrorException: (conn=12) You have an error in your SQL syntax; check the manual that corresponds to your MariaDB server version for the right syntax to use near 'RETURNING `id`' at line 1
```

**Cause:** `returning` of an update on MariaDB before version 13.0, which has no `UPDATE ... RETURNING`. Basepoint
renders the statement, and the database rejects it.

**Fix:** Upgrade to MariaDB 13.0, or leave out `returning` and read the updated rows with a SELECT in the same
transaction.

**Verify:** The update runs, and the SELECT gives the rows as they are after the update.

## Related

- [Insert rows](insert-rows.md)
- [Update rows](update-rows.md)
- [Delete rows](delete-rows.md)
- [Exceptions](../exceptions.md)
