# Query reference

The methods of `Query` and of `Expression`. `Query` is in the package `dev.horizon.basepoint`, and `Expression` in
`dev.horizon.basepoint.expression`. A query is immutable, and every method returns a new query. For the tasks, see
[Queries](README.md).

## `Dialect.collection`

| Parameter | Type     | Description                                                                          |
| --------- | -------- | ------------------------------------------------------------------------------------ |
| `table`   | `String` | The table name. Separate a schema and a table with a dot, for example `shop.orders`. |

Each dot-separated part is quoted on its own: `shop.orders` renders as `"shop"."orders"`. `collection(table)`
returns a `CollectionReference`, whose `query()` starts a query of all rows of the table.

## `Query`

| Method                                                | Description                                                                                  |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `where(Filter filter)`                                | Adds a filter. A row must match all filters.                                                 |
| `where(String text)`                                  | Adds a filter in the AIP-160 syntax. See [Filter text syntax](../filters/filter-text-syntax.md). |
| `columns(String... columns)`                          | Selects only these columns. Default: all columns (`"t0".*`).                                 |
| `select(String column, Expression value)`             | Selects a value as a named column.                                                           |
| `count()`                                             | Selects `COUNT(*)` as the column `count`.                                                    |
| `include(String column, Query rows)`                  | Adds related rows as a JSON array column.                                                    |
| `includeOne(String column, Query row)`                | Adds the first related row as a JSON object column. Adds `LIMIT 1` to `row`.                 |
| `with(String name, Query rows)`                       | Adds a WITH query, which the statement reads like a table named `name`. Only on the outermost query. See [Use WITH queries](use-with-queries.md). |
| `withRecursive(String name, Query start, Query step)` | Adds a recursive WITH query: `WITH RECURSIVE name AS (start UNION ALL step)`.                |
| `on(String column, String outerColumn)`               | Relates this query to the enclosing query: `column = outerColumn`.                           |
| `relation(String name, Query rows)`                   | Names the related row of each row, which `column(name, column)` reads. Writes a `LEFT JOIN`. See [Include related rows](include-related-rows.md#read-a-column-of-the-related-row). |
| `groupBy(String... columns)`                          | Adds `GROUP BY` columns.                                                                     |
| `groupBy(Expression... values)`                       | Adds `GROUP BY` values, such as the column of a relation.                                    |
| `having(Filter filter)`                               | Adds a filter on groups.                                                                     |
| `orderBy(String column)`                              | Sorts ascending by a `select` column of the name, or else a table column.                    |
| `orderBy(String column, Direction direction)`         | Sorts by a `select` column of the name, or else a table column.                              |
| `orderBy(Expression value, Direction direction)`      | Sorts by a value.                                                                            |
| `limit(long rows)`                                    | Limits the number of rows. Must be greater than 0. A later call replaces an earlier one.     |
| `offset(long rows)`                                   | Skips rows. Must be 0 or greater.                                                            |
| `distinct()`                                          | Renders `SELECT DISTINCT`, or `UNION` instead of `UNION ALL` on a union.                     |
| `lock(LockMode mode)`                                 | Locks the rows that the query reads until the transaction ends. See [Dialect reference](../dialects/dialect-reference.md). |
| `union(Query rows)`                                   | Gives the rows of both queries with `UNION ALL`.                                             |
| `intersect(Query rows)`                               | Gives the rows that both queries give, with `INTERSECT`.                                     |
| `except(Query rows)`                                  | Gives the rows of this query that `rows` does not give, with `EXCEPT`.                       |
| `statement()`                                         | The `Statement` tree, before any decorator rewrites it.                                      |
| `build()`                                             | A `RenderedStatement` with the SQL text in `text()` and the placeholder values in `params()`. |

If you call `select` and also want table columns, list the table columns with `columns`.

`union`, `intersect` and `except` return a query of the combined rows. Each method of the result applies to the
combined rows. Operators apply from left to right.

`on` works only in a query that you pass to `include`, `includeOne`, `subquery`, `exists`, `notExists` or
`relation`, or in an operand of a union in one of them. In a query of its own, `build()` throws
`IllegalStateException`.

## `Expression`

| Method                             | SQL                                                                                       |
| ---------------------------------- | ----------------------------------------------------------------------------------------- |
| `column(name)`                     | `"t0"."name"`                                                                             |
| `column(relation, name)`           | A column of a relation of the query: `"t1"."name"`                                        |
| `value(value)`                     | A placeholder, or `NULL` for `null`                                                       |
| `key(column, key)`                 | The value of a key of the JSON object in the column. See [Filter reference](../filters/filter-reference.md#json-keys). |
| `count()`                          | `COUNT(*)`                                                                                |
| `count(column)`                    | `COUNT("t0"."column")`                                                                    |
| `sum(column)`                      | `SUM("t0"."column")`. Also `avg`, `min` and `max`.                                        |
| `subquery(query)`                  | `(SELECT ...)`, a query that selects one column and gives at most one row                 |
| `add(left, right)`                 | `(left + right)`. Also `subtract`, `multiply` and `divide`.                               |
| `concat(texts...)`                 | The texts joined, `NULL` if one of them is `NULL`                                         |
| `lower(text)`                      | `LOWER(text)`. Also `upper` and `trim`.                                                   |
| `length(text)`                     | The number of characters, spaces at the end included                                      |
| `abs(number)`                      | `ABS(number)`                                                                             |
| `coalesce(values...)`              | `COALESCE(values)`, the first value that is not `NULL`                                    |
| `now()`                            | The current time in UTC                                                                   |
| `add(timestamp, duration)`         | The timestamp moved later by a `java.time.Duration`, and `subtract` earlier               |
| `when(condition, then, otherwise)` | `CASE WHEN condition THEN then ELSE otherwise END`                                        |

The SQL of each database for the functions and durations is in [Compute values](compute-values.md#sql-by-database).

## Related

- [Troubleshooting queries](troubleshooting-queries.md)
- [Filter reference](../filters/filter-reference.md)
- [Write reference](../writes/write-reference.md)
- [Exceptions](../exceptions.md)
