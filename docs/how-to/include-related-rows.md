# Include related rows

An include adds related rows to each row of a query as a JSON column. Use it to read an order with its items in one
statement.

| Method                  | Column value                                                               |
| ----------------------- | -------------------------------------------------------------------------- |
| `include(name, rows)`   | A JSON array of objects. An empty array if there are no related rows.      |
| `includeOne(name, row)` | The first related row as a JSON object. `null` if there is no related row. |

For one value of the related rows, such as their number, use `Expression.subquery`, as [Count related rows](#count-related-rows) shows.

## Include a list of rows

1. Create a query of the related table.
2. Call `on` to relate it to the current row. The first argument is the column of the related table. The second
   argument is the column of the enclosing table.
3. Optional: call `columns`, `where`, `orderBy` and `limit` on the related query.
4. Pass the related query to `include`.

```java
Query lines = items.query()
    .on("order_id", "id")
    .columns("id", "sku")
    .orderBy("sku");

RenderedStatement statement = orders.query()
    .columns("id", "state")
    .include("items", lines)
    .build();
```

Each result row has an `items` column with a value such as:

```json
[{"id": 11, "sku": "CUP"}, {"id": 10, "sku": "MUG"}]
```

The array keeps the sort order of the related query.

## Include one row

Use `includeOne` for a relation with at most one related row, such as the customer of an order:

```java
orders.query()
    .columns("id")
    .includeOne("customer", customers.query().on("id", "customer_id").columns("name"));
```

`includeOne` adds `LIMIT 1` to the related query.

## Read a column of the related row

An include gives the related rows as JSON.
To use a column of a related row in the row itself, such as the region of the customer of each order, name the relation with `relation`, and read its columns with `column(relation, column)`:

```java
orders.query()
    .relation("customer", customers.query().on("id", "customer_id"))
    .columns("id")
    .select("region", column("customer", "region"))
    .where(eq(column("customer", "region"), "EU"))
    .orderBy(column("customer", "name"), Direction.ASC);
```

The query writes a `LEFT JOIN` for each relation.
A row whose related row does not exist stays, and its paths read `NULL`.
A filter of the relation's query decides which related row a path reads, not which rows the query gives.

A relation gives at most one related row for each row, such as the customer of an order.
A relation of many rows, such as the items of an order, would repeat the order once for each item: use an include, `exists` or `subquery` for those.

A relation can relate to an earlier one through a path in the outer column of `on`:

```java
items.query()
    .relation("order", orders.query().on("id", "order_id"))
    .relation("archive", archived.query().on("id", "order.id"))
    .select("archived", column("archive", "state"));
```

`groupBy(column("customer", "region"))` groups by a column of the related row.

## Count related rows

`Expression.subquery` turns a related query that gives one value into a value.
Select it, filter by it, or sort by it:

```java
Expression itemCount = subquery(items.query().on("order_id", "id").count());

orders.query()
    .columns("id")
    .select("item_count", itemCount)
    .where(gt(itemCount, 1))
    .orderBy("item_count", Direction.DESC);
```

This renders the subquery `(SELECT COUNT(*) AS "count" FROM "items" AS "t1" WHERE ("t1"."order_id" = "t0"."id"))` in the select list and again in the `WHERE` clause, because `WHERE` cannot read the alias `item_count`.

The query selects one column, and gives at most one row: `count()`, an aggregate such as `select("largest", max("qty"))`, or `limit(1)`.
A query that selects all columns or several columns throws `IllegalArgumentException`.

## Nest includes

A related query can have its own includes:

```java
Query lines = items.query()
    .on("order_id", "id")
    .columns("id", "sku")
    .includeOne("product", products.query().on("sku", "sku").columns("name"));

orders.query().columns("id").include("items", lines);
```

## Select columns on MariaDB and SQLite

PostgreSQL and SQL Server can convert a whole row to JSON. MariaDB and SQLite build each JSON object from named
columns. On MariaDB and SQLite, call `columns` on every included query. Otherwise, `build()` throws
`UnsupportedOperationException`.

## Raise the size limit on MariaDB

MariaDB builds an included array with `JSON_ARRAYAGG`. The result is limited to `group_concat_max_len` bytes, which is
1048576 (1 MB) by default. If an include can return more, raise the limit for the session before you run the query:

```sql
SET SESSION group_concat_max_len = 16777216;
```

## Read the JSON value

The type of the JSON column depends on the database and the client:

| Database   | Column type               |
| ---------- | ------------------------- |
| PostgreSQL | `jsonb`                   |
| MariaDB    | `LONGTEXT` with JSON text |
| SQL Server | `NVARCHAR` with JSON text |
| SQLite     | `TEXT` with JSON text     |

Value formats inside the JSON also depend on the database. For example, MariaDB and SQLite store booleans as integers,
so they appear as `1` and `0`.

## Related pages

- [Builders](../reference/builders.md)
- [Dialects](../reference/dialects.md)
