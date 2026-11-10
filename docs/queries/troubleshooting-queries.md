# Troubleshooting queries

The errors of queries, the most frequent first.

## The column 'id' refers to an enclosing query, but there is none...

```text
java.lang.IllegalStateException: The column 'id' refers to an enclosing query, but there is none. Use on() only in a query that you pass to include(), includeOne(), subquery(), exists() or relation().
```

**Cause:** `build()` of a query with `on`, which relates rows to the row of an enclosing query, but the query runs on
its own.

**Fix:** Pass the query to `include`, `includeOne`, `subquery`, `exists` or `relation`, and build the enclosing
query. To filter a query of its own, use `where` instead of `on`.

**Verify:** The SQL of the enclosing query has the related query inside it, with `= "t0"."id"`.

## The query has no relation named customer. Name it with relation(...

```text
java.lang.IllegalStateException: The query has no relation named customer. Name it with relation("customer", rows) on the query that reads the path.
```

**Cause:** `column("customer", "region")` in a query that has no `relation("customer", ...)`, or that names the
relation on another query.

**Fix:** Call `relation("customer", customers.query().on("id", "customer_id"))` on the query that reads the column.

**Verify:** The SQL has `LEFT JOIN "customers"`, and the column renders as `"t1"."region"`.

## A subquery selects one column, but this query selects 2 columns...

```text
dev.horizon.basepoint.InvalidQueryException: A subquery selects one column, but this query selects 2 columns. Select one with count(), select(name, value) or columns(name).
```

**Cause:** `subquery` of a query that selects all columns or more than one. A subquery is one value.

**Fix:** Select one column, with `count()`, an aggregate in `select`, or `columns` of one name. For several values of
related rows, use `include`.

**Verify:** `subquery(query)` returns, and the SQL has `(SELECT ... AS ...)` in the select list.

## A statement inside another statement cannot have WITH queries...

```text
dev.horizon.basepoint.InvalidQueryException: A statement inside another statement cannot have WITH queries, because SQL Server allows WITH only at the start of a statement. Call with() on the outermost query.
```

**Cause:** `with` on a query that is part of another statement: an include, a subquery, `exists`, a relation, or an
operand of a union.

**Fix:** Call `with` on the outermost query. The queries inside it can read the WITH query by its name.

**Verify:** The SQL starts with `WITH`, and has no other `WITH`.

## The rows of the relation customer have no on(), so every row would...

```text
dev.horizon.basepoint.InvalidQueryException: The rows of the relation customer have no on(), so every row would relate to every row. Relate them with on(column, outerColumn).
```

**Cause:** `relation` of a query without `on`.

**Fix:** Relate the rows with `on`, such as `customers.query().on("id", "customer_id")`.

**Verify:** The SQL has `LEFT JOIN "customers" AS "t1" ON ("t1"."id" = "t0"."customer_id")`.

## The rows of the relation customer can have only on() and filters...

```text
dev.horizon.basepoint.InvalidQueryException: The rows of the relation customer can have only on() and filters. A relation of selected, grouped, sorted or limited rows is not supported.
```

**Cause:** `relation` of a query with `columns`, `select`, `groupBy`, `orderBy`, `limit` or another such call, or of
a union. A relation reads one related row by its columns.

**Fix:** Pass a query with only `on` and `where`, and read its columns with `column(relation, column)`. For several
related rows, use `include`.

**Verify:** The relation renders as a `LEFT JOIN` of the table.

## The outer column order.id reads the relation order, which the query...

```text
dev.horizon.basepoint.InvalidQueryException: The outer column order.id reads the relation order, which the query does not name before it.
```

**Cause:** A relation whose `on` reads a path of a relation that the query names after it, or not at all.

**Fix:** Name the relation of the path first: `relation("order", ...)` before `relation("archive", archived.query()
.on("id", "order.id"))`.

**Verify:** The SQL has the join of `order` before the join of `archive`.

## The combined queries come from different dialects, and the statement...

```text
dev.horizon.basepoint.InvalidQueryException: The combined queries come from different dialects, and the statement renders with one. Read both from collections of the same dialect.
```

**Cause:** `union`, `intersect` or `except` of queries from dialects that are not equal, such as a dialect with
`$1` placeholders and one with `?`, or a dialect with a decorator and one without.

**Fix:** Read both queries from collections of the same dialect, or of equal dialects: `new PostgresDialect()` equals
`new PostgresDialect()`.

**Verify:** The union builds, with the placeholders of the dialect.

## The query has two relations named order.

```text
dev.horizon.basepoint.InvalidQueryException: The query has two relations named order.
```

**Cause:** Two calls of `relation` with one name. `The query has two WITH queries named big.` is the same error for
`with`.

**Fix:** Give each relation or WITH query a name of its own.

**Verify:** The query builds, with one join or WITH query for each name.

## The start of the recursive WITH query tree reads it...

```text
dev.horizon.basepoint.InvalidQueryException: The start of the recursive WITH query tree reads it. Only the step reads it, through a relation.
```

**Cause:** The start and the step of `withRecursive` are in the wrong order, or the start reads the WITH query. The
related errors say that the step does not read the WITH query, or that a part has `on`, a limit or an offset.

**Fix:** Pass the first rows as the start, and the query that relates to the WITH query as the step. Leave out `on`,
`limit` and `offset` in both. See [Use WITH queries](use-with-queries.md#walk-a-tree).

**Verify:** The SQL has `WITH RECURSIVE "tree" AS (... UNION ALL ... JOIN "tree" ...)`.

## The WITH query big relates its rows with on(), but a WITH query has...

```text
dev.horizon.basepoint.InvalidQueryException: The WITH query big relates its rows with on(), but a WITH query has no enclosing row. Leave out on().
```

**Cause:** `with` of a query with `on`.

**Fix:** Leave out `on`. Filter the rows with `where`, or relate them in the query that reads the WITH query.

**Verify:** The SQL starts with `WITH "big" AS (SELECT ...)`.

## An included query cannot select a column named bp_n, which includes...

```text
dev.horizon.basepoint.InvalidQueryException: An included query cannot select a column named bp_n, which includes use to keep the order of their rows. Select it under another name.
```

**Cause:** An include of a query that selects a column or value named `bp_n`. PostgreSQL and SQLite number the rows
of a sorted include in a column of that name.

**Fix:** Select the column under another name, such as `select("number", column("bp_n"))`.

**Verify:** The include builds, and its JSON has the column under the new name.

## The limit must be greater than 0, but is 0.

```text
java.lang.IllegalArgumentException: The limit must be greater than 0, but is 0.
```

**Cause:** `limit(0)` or a negative limit, such as a page size from a request. `offset` refuses a negative offset
with `The offset must be 0 or greater`.

**Fix:** Check the page size before you build the query. To give no rows, filter by `or()`, which matches no row.

**Verify:** The SQL has `LIMIT n` with the page size.

## concat needs at least two texts, but has 1.

```text
java.lang.IllegalArgumentException: concat needs at least two texts, but has 1.
```

**Cause:** `concat` of one text. `coalesce` of one value has the same error.

**Fix:** Pass the one text or value itself, or at least two of them.

**Verify:** The SQL has `||`, `CONCAT` or `+` between the texts.

## Related

- [Query reference](query-reference.md)
- [Troubleshooting filters](../filters/troubleshooting-filters.md)
- [Exceptions](../exceptions.md)
