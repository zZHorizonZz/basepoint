# Write reference

The builders of writes are in the package `dev.horizon.basepoint`. Every builder is immutable, and every method
returns a new builder. For the tasks, see [Insert rows](insert-rows.md), [Update rows](update-rows.md) and
[Delete rows](delete-rows.md).

## `CollectionReference`

| Method                                     | Returns                                                                      |
| ------------------------------------------ | ---------------------------------------------------------------------------- |
| `insert(Map<String, ?> row)`               | An `InsertQuery` of one row. An empty map inserts the default of each column. |
| `insert(List<? extends Map<String, ?>> rows)` | An `InsertQuery` of the rows, which must set the same columns.             |
| `update()`                                 | An `UpdateQuery` of the table                                                |
| `delete()`                                 | A `DeleteQuery` of the table                                                 |

## `InsertQuery`

| Method                         | Description                                                       |
| ------------------------------ | ----------------------------------------------------------------- |
| `ignore(String... key)`        | Skips rows whose key already exists.                              |
| `replace(String... key)`       | Updates existing rows with every inserted column except the key.  |
| `returning(String... columns)` | Returns the inserted rows. Pass no columns to return all columns. |
| `statement()`                  | The `Statement` tree, before any decorator rewrites it.           |
| `build()`                      | The SQL text and its placeholder values.                          |

## `UpdateQuery`

| Method                                    | Description                                             |
| ----------------------------------------- | ------------------------------------------------------- |
| `set(String column, Object value)`        | Sets a column. `null` renders `NULL`.                   |
| `increment(String column, Number amount)` | Renders `column = (column + amount)`.                   |
| `unset(String column)`                    | Sets a column to `NULL`.                                |
| `where(Filter filter)`                    | Adds a filter.                                          |
| `where(String text)`                      | Adds a filter in the AIP-160 syntax.                    |
| `all()`                                   | Allows an update without a filter.                      |
| `returning(String... columns)`            | Returns the updated rows, as they are after the update. |
| `statement()`                             | The `Statement` tree, before any decorator rewrites it. |
| `build()`                                 | The SQL text and its placeholder values.                |

## `DeleteQuery`

| Method                         | Description                                               |
| ------------------------------ | --------------------------------------------------------- |
| `where(Filter filter)`         | Adds a filter.                                            |
| `where(String text)`           | Adds a filter in the AIP-160 syntax.                      |
| `all()`                        | Allows a delete without a filter.                         |
| `returning(String... columns)` | Returns the deleted rows, as they were before the delete. |
| `statement()`                  | The `Statement` tree, before any decorator rewrites it.   |
| `build()`                      | The SQL text and its placeholder values.                  |

## Related

- [Troubleshooting writes](troubleshooting-writes.md)
- [Exceptions](../exceptions.md)
