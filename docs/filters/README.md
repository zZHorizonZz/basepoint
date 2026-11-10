# Filters

A `Filter` chooses the rows that a query, an update or a delete affects. You build filters with the methods of
`Filter`, such as `eq` and `exists`, or write them as text in the AIP-160 syntax, which suits filters that the
users of an API send. Every value becomes a placeholder.

## Features

**Filters in Java.** Compare columns and values, match text, test for `NULL`, read keys of JSON columns and filter
by related rows. See [Filter rows](filter-rows.md) and [Filter reference](filter-reference.md).

**Filter text.** Parse filters such as `state = OPEN AND total > 100` in the AIP-160 syntax, or in CEL. Filter text
reads keys of JSON columns and columns of relations. See [Filter text syntax](filter-text-syntax.md).

The errors of filters and filter text are in [Troubleshooting filters](troubleshooting-filters.md).

## Related

- [Queries](../queries/README.md)
- [Writes](../writes/README.md)
