# Filter text syntax

`where(String)` and `Filter.of(String)` read filters in the [AIP-160](https://google.aip.dev/160) syntax with the
aquery EBNF parser. For the task, see [Filter rows](filter-rows.md). The text parses when you pass it, and translates into the same filters that the Java methods
create when the statement renders.

## Quick reference

| Text                | Filter                                | SQL on PostgreSQL                           |
| ------------------- | ------------------------------------- | ------------------------------------------- |
| `state = OPEN`      | `eq("state", "OPEN")`                 | `("t0"."state" = $1)`                       |
| `state = "OPEN"`    | `eq("state", "OPEN")`                 | `("t0"."state" = $1)`                       |
| `total > 100`       | `gt("total", 100L)`                   | `("t0"."total" > $1)`                       |
| `total >= 1.5`      | `gte("total", new BigDecimal("1.5"))` | `("t0"."total" >= $1)`                      |
| `active = true`     | `eq("active", true)`                  | `("t0"."active" = $1)`                      |
| `note = null`       | `isNull("note")`                      | `("t0"."note" IS NULL)`                     |
| `note != null`      | `isNotNull("note")`                   | `("t0"."note" IS NOT NULL)`                 |
| `note:*`            | `isNotNull("note")`                   | `("t0"."note" IS NOT NULL)`                 |
| `state = "C*"`      | `startsWith("state", "C")`            | `("t0"."state" LIKE $1 ESCAPE '!')`         |
| `a = 1 AND b = 2`   | `and(eq("a", 1L), eq("b", 2L))`       | `(("t0"."a" = $1) AND ("t0"."b" = $2))`     |
| `a = 1 OR b = 2`    | `or(eq("a", 1L), eq("b", 2L))`        | `(("t0"."a" = $1) OR ("t0"."b" = $2))`      |
| `NOT a = 1`         | `not(eq("a", 1L))`                    | `(NOT ("t0"."a" = $1))`                     |
| `-a = 1`            | `not(eq("a", 1L))`                    | `(NOT ("t0"."a" = $1))`                     |
| `labels.env = prod` | `eq(key("labels", "env"), "prod")`    | `(("t0"."labels" ->> CAST($1 AS text)) = $2)` |
| `labels.env:*`      | `ne(key("labels", "env"), null)`      | `(("t0"."labels" ->> CAST($1 AS text)) IS NOT NULL)` |
| `labels:env`        | `ne(key("labels", "env"), null)`      | `(("t0"."labels" ->> CAST($1 AS text)) IS NOT NULL)` |
| (empty text)        | `and()`                               | `(1 = 1)`                                   |

## Operators

| Operator                | Meaning                                                                                       |
| ----------------------- | --------------------------------------------------------------------------------------------- |
| `=`                     | Equal                                                                                         |
| `!=`                    | Not equal                                                                                     |
| `<`, `<=`, `>`, `>=`    | Ordered comparison                                                                            |
| `:`                     | Has. `column:*` and `column.key:*` test for a value, `column:key` for a key of a JSON object. |
| `AND`, `OR`, `NOT`, `-` | Logic                                                                                         |
| `( )`                   | Grouping, at most 64 levels deep                                                              |

Terms without an operator between them must all match, as with `AND`. `OR` binds before `AND`, as AIP-160 says:
`a AND b OR c` is `a AND (b OR c)`.

## Keys of JSON objects

A field of two parts, such as `labels.env`, is the value of a key of the JSON object in a column, as
`Expression.key("labels", "env")` gives it. Its values compare as text, so `labels.env = 5` compares with the string
`"5"`. It supports `=`, `!=`, `:*` and `*` in quoted text. `labels:env` matches the rows whose object has the key
`env`, which is how [AIP-160](https://google.aip.dev/160#has-operator) tests a map for a key. The SQL of each
database is in [Dialect reference](../dialects/dialect-reference.md#differences-by-database).

## Columns of relations

In the filter text of a query with a relation, a field of two parts whose first part names the relation is a column
of the related row: `customer.region = EU` reads the column `region` of the relation `customer`, as
`column("customer", "region")` gives it. It compares as a column does, so it supports every operator, and
`customer.region:*` matches the rows whose related row has a value in it.

The text translates when the statement renders, with the relations of the statement and of the statements around
it, so the order of `where` and `relation` does not matter. A relation hides a JSON column of the same name, so give
relations names that no JSON column has.

## Values

| Form                                         | Java type                                              |
| -------------------------------------------- | ------------------------------------------------------ |
| Unquoted integer, for example `100`          | `Long`, or `BigDecimal` if it does not fit in a `long` |
| Unquoted decimal, for example `1.5` or `2e3` | `BigDecimal`                                           |
| `true`, `false`                              | `Boolean`                                              |
| `null`                                       | No value. Renders `IS NULL` or `IS NOT NULL`.          |
| Any other unquoted text, for example `OPEN`  | `String`                                               |
| Quoted text, for example `"OPEN"`            | `String`                                               |

A `*` in quoted text matches any text. For example, `name = "A*Z"` matches values that start with `A` and end with
`Z`. Characters such as `%` and `_` match only themselves. `"null"` in quotes is the text `null`.

Values that contain a colon, such as timestamps, must be quoted. A quoted timestamp is a `String`. To compare a
timestamp column with a typed value, use a Java filter, for example `gt("created", Instant.parse(...))`.

## Errors

Text that does not parse throws `dev.horizon.aquery.InvalidQueryException` when you pass it. Text that parses but has
no translation throws `dev.horizon.aquery.aip160.InvalidFilterException`, which extends it, when the statement
renders. Each message, with its cause and fix, is in [Troubleshooting filters](troubleshooting-filters.md).

## CEL

To write filters in CEL, add the `dev.horizon:aquery-grammar-cel` dependency and pass its parser:

```java
Filter open = Filter.of("state == 'OPEN' && total > 100", new CelFilterParser());
```

## Related

- [Filter reference](filter-reference.md)
- [Filter rows](filter-rows.md)
- [Troubleshooting filters](troubleshooting-filters.md)
