# Exceptions

The exceptions that Basepoint throws, and when. The exceptions of Basepoint are in `dev.horizon.basepoint`. Each
extends the standard exception of its kind, so a `catch` of `IllegalArgumentException` catches an
`InvalidQueryException` too. For each message and its fix, see the troubleshooting page of its area.

| Exception                                          | Extends                         | Thrown when                                                                                                    | Troubleshooting |
| -------------------------------------------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------- | --------------- |
| `InvalidQueryException`                            | `IllegalArgumentException`      | Queries cannot be put together as asked: a relation of selected rows, a subquery of two columns, a WITH query inside another statement, a union of two dialects, an include of a column named `bp_n`. | [Troubleshooting queries](queries/troubleshooting-queries.md) |
| `UnfilteredWriteException`                         | `IllegalStateException`         | `build()` of an update or delete without a filter and without `all()`.                                         | [Troubleshooting writes](writes/troubleshooting-writes.md) |
| `UnsupportedFeatureException`                      | `UnsupportedOperationException` | The database cannot render the statement: an include of all columns on MariaDB and SQLite, a duration beyond 68 years on SQL Server, an include on a dialect without decorators for it. | [Troubleshooting dialects](dialects/troubleshooting-dialects.md) |
| `IllegalArgumentException`                         |                                 | An argument is invalid: a limit of 0, a negative offset, rows that set different columns, an upsert without a key, a key of a JSON object with `"`. | [Troubleshooting writes](writes/troubleshooting-writes.md) |
| `IllegalStateException`                            |                                 | `build()` of an update without assignments, of `on` in a query of its own, or of a path to a relation that the query does not name. | [Troubleshooting queries](queries/troubleshooting-queries.md) |
| `dev.horizon.aquery.InvalidQueryException`         | `RuntimeException`              | Filter text does not parse, when you pass it to `where` or `Filter.of`.                                        | [Troubleshooting filters](filters/troubleshooting-filters.md) |
| `dev.horizon.aquery.aip160.InvalidFilterException` | `dev.horizon.aquery.InvalidQueryException` | Filter text parses but has no translation, when the statement renders.                              | [Troubleshooting filters](filters/troubleshooting-filters.md) |

## Related

- [Basepoint](README.md)
