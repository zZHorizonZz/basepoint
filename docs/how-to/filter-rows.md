# Filter rows

Use `where` to select the rows that a query, update or delete affects. Each call to `where` adds a filter. A row
must match all filters.

The examples on this page use these imports and references:

```java
import static dev.horizon.basepoint.Expression.column;
import static dev.horizon.basepoint.Filter.*;

Dialect dialect = new PostgresDialect();
CollectionReference orders = dialect.collection("orders");
CollectionReference items = dialect.collection("items");
```

## Build a filter in Java

1. Choose a filter method from `Filter`, such as `eq`, `gt` or `in`.
2. Combine filters with `and`, `or` and `not`.
3. Pass the filter to `where`.

```java
RenderedStatement statement = orders.query()
    .where(or(eq("region", "US"), gte("total", 300)))
    .build();
```

Every value becomes a placeholder. Basepoint never writes a value into the SQL text.

## Write a filter as text

Pass a string to `where` to use the AIP-160 filter syntax:

```java
orders.query().where("state = \"OPEN\" AND total > 100");
```

This renders the same SQL as the Java filter `and(eq("state", "OPEN"), gt("total", 100))`. For the full syntax, see
[Filter text syntax](../reference/filter-text-syntax.md).

To use CEL instead, add the `aquery-grammar-cel` dependency and pass its parser to `Filter.of`:

```java
Filter open = Filter.of("state == 'OPEN' && total > 100", new CelFilterParser());
```

## Compare two columns

Pass `column(name)` as the value:

```java
items.query().where(gt("qty", column("min_qty")));
```

## Filter by NULL

Pass `null` to `eq` or `ne`, or use `isNull` and `isNotNull`:

```java
orders.query().where(eq("note", null));    // "note" IS NULL
orders.query().where(isNotNull("note"));   // "note" IS NOT NULL
```

## Match text

`startsWith`, `endsWith` and `contains` match literal text. Characters such as `%` and `_` in the text match only
themselves.

```java
orders.query().where(startsWith("note", "50%"));
```

This binds the pattern `50!%%` and renders `LIKE ? ESCAPE '!'`.

To use database wildcards, call `like` with your own pattern:

```java
orders.query().where(like("sku", "M%"));
```

Case sensitivity depends on the collation of the column. For example, MariaDB and SQL Server compare text without
case by default.

## Filter by a key of a JSON column

Use `Expression.key` for the value of a key of the JSON object in a column:

```java
import static dev.horizon.basepoint.Expression.key;

orders.query().where(eq(key("labels", "env"), "prod"));   // labels has env = "prod"
orders.query().where(ne(key("labels", "env"), null));     // labels has the key env
```

On PostgreSQL, this renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."labels" ->> CAST($1 AS text)) = $2)
```

MariaDB and SQL Server render `JSON_VALUE("t0"."labels", ?)`, and SQLite `json_extract("t0"."labels", ?)`, with the
path `$."env"` bound.

## Filter by related rows

Use `exists` or `notExists` with a query of the related table. Call `on` to relate the subquery to the current row:

```java
orders.query().where(exists(items.query().on("order_id", "id").where(gt("qty", 5))));
```

This renders:

```sql
SELECT "t0".* FROM "orders" AS "t0"
WHERE EXISTS (SELECT 1 FROM "items" AS "t1" WHERE (("t1"."order_id" = "t0"."id") AND ("t1"."qty" > $1)))
```

## Related pages

- [Filters](../reference/filters.md)
- [Dialects](../reference/dialects.md)
- [Filter text syntax](../reference/filter-text-syntax.md)
