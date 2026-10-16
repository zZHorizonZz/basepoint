# Builders

Builders create statements. Every builder is immutable, and every method returns a new builder. All types are in
the package `dev.horizon.basepoint`.

## Quick reference

| Type                  | Created by                                          | Statement                        |
| --------------------- | --------------------------------------------------- | -------------------------------- |
| `CollectionReference` | `Dialect.collection(table)`                         | None. Starts the other builders. |
| `Query`               | `CollectionReference.query()`                       | `SELECT`                         |
| `CompoundQuery`       | `Query.union`, `Query.intersect`, `Query.except`    | `UNION`, `INTERSECT`, `EXCEPT`   |
| `InsertQuery`         | `CollectionReference.insert(row)` or `insert(rows)` | `INSERT`, or an upsert           |
| `UpdateQuery`         | `CollectionReference.update()`                      | `UPDATE`                         |
| `DeleteQuery`         | `CollectionReference.delete()`                      | `DELETE`                         |

Every builder except `CollectionReference` has these methods:

| Method        | Returns                                                                                      |
| ------------- | -------------------------------------------------------------------------------------------- |
| `statement()` | The `Statement` tree, before any decorator rewrites it                                       |
| `build()`     | A `RenderedStatement` with the SQL text in `text()` and the placeholder values in `params()` |

## `Dialect.collection`

| Parameter | Type     | Description                                                                          |
| --------- | -------- | ------------------------------------------------------------------------------------ |
| `table`   | `String` | The table name. Separate a schema and a table with a dot, for example `shop.orders`. |

Each dot-separated part is quoted on its own: `shop.orders` renders as `"shop"."orders"`.

## `Query`

| Method                                           | Description                                                                                  |
| ------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| `where(Filter filter)`                           | Adds a filter. A row must match all filters.                                                 |
| `where(String filter)`                           | Adds a filter in the AIP-160 syntax. See [Filter text syntax](filter-text-syntax.md).        |
| `columns(String... columns)`                     | Selects only these columns. Default: all columns (`"t0".*`).                                 |
| `select(String column, Expression value)`        | Selects a value as a named column.                                                           |
| `count()`                                        | Selects `COUNT(*)` as the column `count`.                                                    |
| `include(String column, Query rows)`             | Adds related rows as a JSON array column.                                                    |
| `includeOne(String column, Query row)`           | Adds the first related row as a JSON object column. Adds `LIMIT 1` to `row`.                 |
| `includeCount(String column, Query rows)`        | Adds the number of related rows as a column.                                                 |
| `on(String column, String outerColumn)`          | Relates this query to the enclosing query: `column = outerColumn`.                           |
| `groupBy(String... columns)`                     | Adds `GROUP BY` columns.                                                                     |
| `having(Filter filter)`                          | Adds a filter on groups.                                                                     |
| `orderBy(String column)`                         | Sorts ascending by a table column or a `select` column.                                      |
| `orderBy(String column, Direction direction)`    | Sorts by a table column or a `select` column.                                                |
| `orderBy(Expression value, Direction direction)` | Sorts by a value.                                                                            |
| `limit(long rows)`                               | Limits the number of rows. Must be greater than 0.                                           |
| `offset(long rows)`                              | Skips rows. Must be 0 or greater.                                                            |
| `distinct()`                                     | Renders `SELECT DISTINCT`.                                                                   |
| `lock(LockMode mode)`                            | Locks the rows that the query reads until the transaction ends. See [Dialects](dialects.md). |
| `union(Combinable other)`                        | Combines with `UNION ALL`.                                                                   |
| `intersect(Combinable other)`                    | Combines with `INTERSECT`.                                                                   |
| `except(Combinable other)`                       | Combines with `EXCEPT`.                                                                      |

If you call `select` and also want table columns, list the table columns with `columns`.

`on` works only in a query that you pass to `include`, `includeOne`, `includeCount`, `Filter.exists` or
`Filter.notExists`. In a top-level query, `build()` throws `IllegalStateException`.

## `CompoundQuery`

| Method                                        | Description                                                                |
| --------------------------------------------- | -------------------------------------------------------------------------- |
| `distinct()`                                  | Changes `UNION ALL` to `UNION`. Has no effect on `INTERSECT` and `EXCEPT`. |
| `orderBy(String column)`                      | Sorts the combined rows ascending.                                         |
| `orderBy(String column, Direction direction)` | Sorts the combined rows.                                                   |
| `limit(long rows)`                            | Limits the combined rows.                                                  |
| `offset(long rows)`                           | Skips combined rows.                                                       |
| `union`, `intersect`, `except`                | Adds another operator. Operators apply from left to right.                 |

## `InsertQuery`

| Method                         | Description                                                       |
| ------------------------------ | ----------------------------------------------------------------- |
| `ignore(String... key)`        | Skips rows whose key already exists.                              |
| `replace(String... key)`       | Updates existing rows with every inserted column except the key.  |
| `returning(String... columns)` | Returns the inserted rows. Pass no columns to return all columns. |

`CollectionReference.insert` throws `IllegalArgumentException` in these cases:

| Case                           | Message                                                                                            |
| ------------------------------ | -------------------------------------------------------------------------------------------------- |
| The list of rows is empty      | `The insert has no rows.`                                                                          |
| Two rows set different columns | `Row 1 sets the columns [...], but the first row sets [...]. Every row must set the same columns.` |
| More than one row is empty     | `Only one row can use the default value of every column.`                                          |

## `UpdateQuery`

| Method                                    | Description                                             |
| ----------------------------------------- | ------------------------------------------------------- |
| `set(String column, Object value)`        | Sets a column. `null` renders `NULL`.                   |
| `increment(String column, Number amount)` | Renders `column = (column + amount)`.                   |
| `unset(String column)`                    | Sets a column to `NULL`.                                |
| `where(Filter filter)`                    | Adds a filter.                                          |
| `where(String filter)`                    | Adds a filter in the AIP-160 syntax.                    |
| `all()`                                   | Allows an update without a filter.                      |
| `returning(String... columns)`            | Returns the updated rows, as they are after the update. |

A later assignment to the same column replaces the earlier one.

`build()` throws `IllegalStateException` if the update sets no columns, or if it has no filter and no `all()`.

## `DeleteQuery`

| Method                         | Description                                               |
| ------------------------------ | --------------------------------------------------------- |
| `where(Filter filter)`         | Adds a filter.                                            |
| `where(String filter)`         | Adds a filter in the AIP-160 syntax.                      |
| `all()`                        | Allows a delete without a filter.                         |
| `returning(String... columns)` | Returns the deleted rows, as they were before the delete. |

`build()` throws `IllegalStateException` if the delete has no filter and no `all()`.

## `Expression`

| Method          | SQL                    |
| --------------- | ---------------------- |
| `column(name)`  | `"t0"."name"`          |
| `value(value)`  | A placeholder          |
| `count()`       | `COUNT(*)`             |
| `count(column)` | `COUNT("t0"."column")` |
| `sum(column)`   | `SUM("t0"."column")`   |
| `avg(column)`   | `AVG("t0"."column")`   |
| `min(column)`   | `MIN("t0"."column")`   |
| `max(column)`   | `MAX("t0"."column")`   |
