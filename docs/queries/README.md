# Queries

A `Query` reads rows: the rows of a table, or the rows of several queries combined. It selects columns and computed
values, includes related rows as JSON, groups, sorts and pages, and renders one SELECT statement. Start a query with
`dialect.collection(table).query()`.

## Features

**Related rows.** Add the related rows of each row as a JSON column, such as the items of an order, or read a column
of one related row, such as the region of its customer. See [Include related rows](include-related-rows.md).

**Groups and combinations.** Aggregate rows by groups, remove duplicates, and combine the rows of queries with
`union`, `intersect` and `except`. See [Group and combine rows](group-and-combine-rows.md).

**Computed values.** Select, filter and sort by functions, arithmetic, conditions, moved timestamps and the values of
subqueries. See [Compute values](compute-values.md).

**WITH queries.** Name the rows of a query so that the statement reads them like a table, and walk a tree with a
recursive WITH query. See [Use WITH queries](use-with-queries.md).

Every method of `Query` and every expression is in [Query reference](query-reference.md). The errors of queries
are in [Troubleshooting queries](troubleshooting-queries.md).

## Related

- [Filters](../filters/README.md)
- [Dialects](../dialects/README.md)
