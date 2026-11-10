# Filter rows

Filter the rows that a query, an update or a delete affects with `where`. Build the filter in Java when your code
decides it, and write it as [filter text](filter-text-syntax.md) when the users of an API send it.

## Before you begin

The examples on this page use these imports and references:

```java
import static dev.horizon.basepoint.expression.Expression.*;
import static dev.horizon.basepoint.filter.Filter.*;

Dialect dialect = new PostgresDialect();
CollectionReference orders = dialect.collection("orders");
CollectionReference items = dialect.collection("items");
```

## Filter a query

1. Choose a filter method from `Filter` for each condition, such as `eq`, `gt` or `in`, and combine the conditions
   with `and`, `or` and `not`:

   ```java
   Filter usOrLarge = or(eq("region", "US"), gte("total", 300));
   ```

2. Pass the filter to `where`. Each call of `where` adds a filter, and a row must match all of them:

   ```java
   orders.query().where(usOrLarge);
   ```

The query renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."region" = $1) OR ("t0"."total" >= $2))
```

Every value is a placeholder. Basepoint never writes a value into the SQL text.

## Write the filter as text

Pass a string to `where` to use the AIP-160 filter syntax:

```java
orders.query().where("state = \"OPEN\" AND total > 100");
```

This renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."state" = $1) AND ("t0"."total" > $2))
```

That is the SQL of the Java filter `and(eq("state", "OPEN"), gt("total", 100))`. Empty text matches every row. For
the full syntax, see [Filter text syntax](filter-text-syntax.md).

To use CEL instead, add the `aquery-grammar-cel` dependency and pass its parser to `Filter.of`:

```java
Filter open = Filter.of("state == 'OPEN' && total > 100", new CelFilterParser());
```

## Compare a value

Every method that takes a column name also takes an `Expression`, such as a function of a column or an aggregate.
A value can be an `Expression` too, such as another column:

```java
items.query().where(gt("qty", column("min_qty")));
```

This renders:

```sql
SELECT "t0".* FROM "items" AS "t0" WHERE ("t0"."qty" > "t0"."min_qty")
```

```java
orders.query().where(startsWith(lower(column("state")), "op"));
```

This renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (LOWER("t0"."state") LIKE $1 ESCAPE '!')
```

## Filter by NULL

Pass `null` to `eq` or `ne`, or use `isNull` and `isNotNull`:

```java
orders.query().where(eq("note", null));
```

This renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE ("t0"."note" IS NULL)
```

A `null` in the values of `in` matches the rows where the column is `NULL` too:

```java
orders.query().where(in("note", Arrays.asList("rush", null)));
```

This renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."note" IN ($1)) OR ("t0"."note" IS NULL))
```

## Match text

`startsWith`, `endsWith` and `contains` match literal text. Characters such as `%`, `_`, `[` and `!` in the text
match only themselves.

```java
orders.query().where(startsWith("note", "50%"));
```

This binds the pattern `50!%%` and renders `LIKE $1 ESCAPE '!'`.

To use database wildcards, call `like` with your own pattern:

```java
orders.query().where(like("sku", "M%"));
```

Case sensitivity depends on the collation of the column. For example, MariaDB and SQL Server compare text without
case by default. For characters such as emoji, see
[Troubleshooting dialects](../dialects/troubleshooting-dialects.md).

## Filter by a key of a JSON column

Use `key` for the value of a key of the JSON object in a column:

```java
orders.query().where(eq(key("labels", "env"), "prod"));
```

On PostgreSQL, this renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."labels" ->> CAST($1 AS text)) = $2)
```

`ne(key("labels", "env"), null)` matches the rows whose object has the key `env`. MariaDB renders
``JSON_VALUE(`t0`.`labels`, ?)``, SQL Server `JSON_VALUE([t0].[labels], @p1)`, and SQLite
`CAST(json_extract("t0"."labels", ?) AS TEXT)`, with the path `$."env"` bound.

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

To filter by a column of a related row, such as the region of the customer of an order, name the relation with
`relation` and compare `column(relation, column)`. See
[Include related rows](../queries/include-related-rows.md#read-a-column-of-the-related-row).

## Related

- [Filter reference](filter-reference.md)
- [Filter text syntax](filter-text-syntax.md)
- [Troubleshooting filters](troubleshooting-filters.md)
