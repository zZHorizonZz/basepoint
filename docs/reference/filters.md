# Filters

The static methods of `dev.horizon.basepoint.Filter` create filters for `where` and `having`. Every value becomes
a placeholder.

Each method that takes a column name also has an overload that takes an `Expression`, for example
`gt(sum("total"), 1000)`. A value can also be an `Expression`, for example `gt("qty", column("min_qty"))`.

## Comparison

| Method                       | SQL                        | Notes                                                                                                             |
| ---------------------------- | -------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `eq(column, value)`          | `(column = ?)`             | `eq(column, null)` renders `(column IS NULL)`.                                                                    |
| `ne(column, value)`          | `(column <> ?)`            | `ne(column, null)` renders `(column IS NOT NULL)`. Rows where the column is `NULL` do not match a non-null value. |
| `gt(column, value)`          | `(column > ?)`             |                                                                                                                   |
| `gte(column, value)`         | `(column >= ?)`            |                                                                                                                   |
| `lt(column, value)`          | `(column < ?)`             |                                                                                                                   |
| `lte(column, value)`         | `(column <= ?)`            |                                                                                                                   |
| `between(column, low, high)` | `(column BETWEEN ? AND ?)` | Inclusive.                                                                                                        |

## Lists

| Method                | SQL                      | Empty collection                   |
| --------------------- | ------------------------ | ---------------------------------- |
| `in(column, values)`  | `(column IN (?, ?))`     | `(1 = 0)`, which matches no rows   |
| `nin(column, values)` | `(column NOT IN (?, ?))` | `(1 = 1)`, which matches every row |

`in` and `nin` do not accept `null` elements.

## Null checks

| Method              | SQL                    |
| ------------------- | ---------------------- |
| `isNull(column)`    | `(column IS NULL)`     |
| `isNotNull(column)` | `(column IS NOT NULL)` |

## Text

| Method                     | SQL                          | Bound pattern for `"50%"`  |
| -------------------------- | ---------------------------- | -------------------------- |
| `startsWith(column, text)` | `(column LIKE ? ESCAPE '!')` | `50!%%`                    |
| `endsWith(column, text)`   | `(column LIKE ? ESCAPE '!')` | `%50!%`                    |
| `contains(column, text)`   | `(column LIKE ? ESCAPE '!')` | `%50!%%`                   |
| `like(column, pattern)`    | `(column LIKE ?)`            | The pattern as you pass it |

`startsWith`, `endsWith` and `contains` escape `%`, `_`, `[` and `!` in the text. `like` does not escape the
pattern.

Case sensitivity depends on the collation of the column.

## JSON keys

`Expression.key(column, key)` is the value of a key of the JSON object in a column, or `NULL` if the object has no
such key. Pass it to the methods that take an `Expression`:

| Filter                             | Matches the rows whose object    |
| ---------------------------------- | -------------------------------- |
| `eq(key("labels", "env"), "prod")` | has `env` with the value `prod`  |
| `ne(key("labels", "env"), "prod")` | has `env` with another value     |
| `in(key("labels", "env"), values)` | has `env` with one of the values |
| `ne(key("labels", "env"), null)`   | has `env`                        |
| `eq(key("labels", "env"), null)`   | has no `env`, or is `NULL`       |

The value is a string. A key must not be empty or contain `"` or `\`. Each dialect renders the key with the JSON
functions of its database; see [Dialects](dialects.md#differences-by-database).

## Logic

| Method            | SQL         | With no filters |
| ----------------- | ----------- | --------------- |
| `and(filters...)` | `(a AND b)` | `(1 = 1)`       |
| `or(filters...)`  | `(a OR b)`  | `(1 = 0)`       |
| `not(filter)`     | `(NOT a)`   |                 |

## Related rows

| Method             | SQL                                          |
| ------------------ | -------------------------------------------- |
| `exists(query)`    | `EXISTS (SELECT 1 FROM ... WHERE ...)`       |
| `notExists(query)` | `(NOT EXISTS (SELECT 1 FROM ... WHERE ...))` |

Create the query from a `CollectionReference`, and relate it to the current row with `Query.on`.

## Text filters

| Method             | Description                                                                             |
| ------------------ | --------------------------------------------------------------------------------------- |
| `of(text)`         | Parses a filter in the AIP-160 syntax. See [Filter text syntax](filter-text-syntax.md). |
| `of(text, parser)` | Parses a filter with an aquery parser, for example `new CelFilterParser()`.             |

## Custom filters

`Filter` is a functional interface. To add your own filter, implement `render(StatementWriter)`:

```java
Filter recent = sql -> sql.append("(").column("created").append(" > now() - interval '1 day')");
```
