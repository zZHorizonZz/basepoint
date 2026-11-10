# Filter text syntax

`where(String)` and `Filter.of(String)` parse filters in the [AIP-160](https://google.aip.dev/160) syntax with the
aquery EBNF parser. The result is the same `Filter` that the Java methods create.

## Quick reference

| Text                | Filter                                | SQL                                          |
| ------------------- | ------------------------------------- | -------------------------------------------- |
| `state = OPEN`      | `eq("state", "OPEN")`                 | `("t0"."state" = ?)`                         |
| `state = "OPEN"`    | `eq("state", "OPEN")`                 | `("t0"."state" = ?)`                         |
| `total > 100`       | `gt("total", 100L)`                   | `("t0"."total" > ?)`                         |
| `total >= 1.5`      | `gte("total", new BigDecimal("1.5"))` | `("t0"."total" >= ?)`                        |
| `active = true`     | `eq("active", true)`                  | `("t0"."active" = ?)`                        |
| `note = null`       | `isNull("note")`                      | `("t0"."note" IS NULL)`                      |
| `note != null`      | `isNotNull("note")`                   | `("t0"."note" IS NOT NULL)`                  |
| `note:*`            | `isNotNull("note")`                   | `("t0"."note" IS NOT NULL)`                  |
| `state = "C*"`      | `startsWith("state", "C")`            | `("t0"."state" LIKE ? ESCAPE '!')`           |
| `a = 1 AND b = 2`   | `and(eq("a", 1L), eq("b", 2L))`       | `(("t0"."a" = ?) AND ("t0"."b" = ?))`        |
| `a = 1 OR b = 2`    | `or(eq("a", 1L), eq("b", 2L))`        | `(("t0"."a" = ?) OR ("t0"."b" = ?))`         |
| `NOT a = 1`         | `not(eq("a", 1L))`                    | `(NOT ("t0"."a" = ?))`                       |
| `-a = 1`            | `not(eq("a", 1L))`                    | `(NOT ("t0"."a" = ?))`                       |
| `labels.env = prod` | `eq(key("labels", "env"), "prod")`    | `(JSON_VALUE("t0"."labels", ?) = ?)`         |
| `labels.env:*`      | `ne(key("labels", "env"), null)`      | `(JSON_VALUE("t0"."labels", ?) IS NOT NULL)` |
| `labels:env`        | `ne(key("labels", "env"), null)`      | `(JSON_VALUE("t0"."labels", ?) IS NOT NULL)` |

## Operators

| Operator                | Meaning                                                                                       |
| ----------------------- | --------------------------------------------------------------------------------------------- |
| `=`                     | Equal                                                                                         |
| `!=`                    | Not equal                                                                                     |
| `<`, `<=`, `>`, `>=`    | Ordered comparison                                                                            |
| `:`                     | Has. `column:*` and `column.key:*` test for a value, `column:key` for a key of a JSON object. |
| `AND`, `OR`, `NOT`, `-` | Logic                                                                                         |
| `( )`                   | Grouping                                                                                      |

## Keys of JSON objects

A field of two parts, such as `labels.env`, is the value of a key of the JSON object in a column, as
`Expression.key("labels", "env")` gives it. Its values compare as text, so `labels.env = 5` compares with the string
`"5"`. It supports `=`, `!=`, `:*` and `*` in quoted text. `labels:env` matches the rows whose object has the key
`env`, which is how [AIP-160](https://google.aip.dev/160#has-operator) tests a map for a key. The SQL of each
database is in [Dialects](dialects.md#differences-by-database).

## Columns of relations

In the filter text of a query with a relation, a field of two parts whose first part names the relation is a column
of the related row: `customer.region = EU` reads the column `region` of the relation `customer`, as
`column("customer", "region")` gives it. It compares as a column does, so it supports every operator, and
`customer.region:*` matches the rows whose related row has a value in it.

The query must name the relation before the filter text: text before it reads `customer.region` as a key of a JSON
column, and `relation("customer", ...)` after it throws `IllegalArgumentException`. A relation hides a JSON column of
the same name, so give relations names that no JSON column has. This applies to `Query.where(String)`. A filter from
`Filter.of`, which knows no query, reads every field of two parts as a key of a JSON column.

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
`Z`. Characters such as `%` and `_` match only themselves.

Values that contain a colon, such as timestamps, must be quoted. A quoted timestamp is a `String`. To compare a
timestamp column with a typed value, use a Java filter, for example `gt("created", Instant.parse(...))`.

## Errors

Parsing throws `dev.horizon.aquery.InvalidQueryException` in these cases:

| Text           | Message                                                                                                                      |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `plain`        | `The filter term 'plain' has no column. Compare a column with a value, for example state = OPEN.`                            |
| `a.b.c = 1`    | `Unsupported field path 'a.b.c'. Use a column name, a column and a key of the JSON object in it, such as labels.env, or a relation and its column, such as customer.region.` |
| `a.b > 1`      | `Unsupported operator '>' on a key of a JSON object, whose values compare only as text.`                                     |
| `total > null` | `Cannot compare null with >. Use = or != with null.`                                                                         |

## CEL

To write filters in CEL, add the `dev.horizon:aquery-grammar-cel` dependency and pass its parser:

```java
Filter open = Filter.of("state == 'OPEN' && total > 100", new CelFilterParser());
```
