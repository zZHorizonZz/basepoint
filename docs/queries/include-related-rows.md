# Include related rows

Add the related rows of each row as a JSON column, such as an order with its items, in one statement. Use an
include for a list of related rows. For one value of them, such as their number, use a
[subquery](#count-related-rows), and for a column of one related row, use a
[relation](#read-a-column-of-the-related-row).

| Method                  | Column value                                                               |
| ----------------------- | -------------------------------------------------------------------------- |
| `include(name, rows)`   | A JSON array of objects. An empty array if there are no related rows.      |
| `includeOne(name, row)` | The first related row as a JSON object. `null` if there is no related row. |

## Before you begin

The related table needs a column that points to the row, such as `items.order_id` to `orders.id`. On MariaDB and
SQLite, the included query must list its columns; see
[Select columns on MariaDB and SQLite](#select-columns-on-mariadb-and-sqlite).

## Include a list of rows

1. Start a query of the related table, and relate it to the current row with `on`. The first argument is the column
   of the related table, and the second the column of the enclosing table:

   ```java
   Query lines = items.query().on("order_id", "id");
   ```

2. Optional: choose the columns, the filter, the order and the limit of the related rows:

   ```java
   lines = lines.columns("id", "sku").orderBy("sku");
   ```

3. Pass the related query to `include`, with the name of the result column:

   ```java
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

An include gives the related rows as JSON. To use a column of a related row in the row itself, such as the region of
the customer of each order, name the relation with `relation`, and read its columns with `column(relation, column)`:

```java
orders.query()
    .relation("customer", customers.query().on("id", "customer_id"))
    .columns("id")
    .select("region", column("customer", "region"))
    .where(eq(column("customer", "region"), "EU"));
```

This renders:

```sql
SELECT "t0"."id", "t1"."region" AS "region" FROM "orders" AS "t0"
LEFT JOIN "customers" AS "t1" ON ("t1"."id" = "t0"."customer_id") WHERE ("t1"."region" = $1)
```

The query writes a `LEFT JOIN` for each relation. A row whose related row does not exist stays, and its paths read
`NULL`. A filter of the relation's query decides which related row a path reads, not which rows the query gives.
Filter text reads the relation too: `where("customer.region = EU")` is the same filter, before or after the call of
`relation`.

A relation gives at most one related row for each row, such as the customer of an order. A relation of many rows,
such as the items of an order, would repeat the order once for each item: use an include, `exists` or `subquery`
for those.

A relation can relate to an earlier one through a path in the outer column of `on`:

```java
items.query()
    .relation("order", orders.query().on("id", "order_id"))
    .relation("archive", archived.query().on("id", "order.id"))
    .select("archived", column("archive", "state"));
```

`groupBy(column("customer", "region"))` groups by a column of the related row.

## Count related rows

`subquery` turns a related query that gives one value into a value. Select it, filter by it, or sort by it:

```java
Expression itemCount = subquery(items.query().on("order_id", "id").count());

orders.query()
    .columns("id")
    .select("item_count", itemCount)
    .where(gt(itemCount, 1))
    .orderBy("item_count", Direction.DESC);
```

This renders:

```sql
SELECT "t0"."id", (SELECT COUNT(*) AS "count" FROM "items" AS "t1" WHERE ("t1"."order_id" = "t0"."id"))
AS "item_count" FROM "orders" AS "t0"
WHERE ((SELECT COUNT(*) AS "count" FROM "items" AS "t2" WHERE ("t2"."order_id" = "t0"."id")) > $1)
ORDER BY "item_count" DESC
```

The subquery renders twice, because `WHERE` cannot read the alias `item_count`. The query must select one column and
give at most one row: `count()`, an aggregate such as `select("largest", max("qty"))`, or `limit(1)`.

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
columns, so on MariaDB and SQLite, call `columns` on every included query.

## Read the JSON value

The type of the JSON column depends on the database and the client:

| Database   | Column type               |
| ---------- | ------------------------- |
| PostgreSQL | `jsonb`                   |
| MariaDB    | `LONGTEXT` with JSON text |
| SQL Server | `NVARCHAR` with JSON text |
| SQLite     | `TEXT` with JSON text     |

Value formats inside the JSON also depend on the database. For example, MariaDB and SQLite store booleans as integers,
so they appear as `1` and `0`. MariaDB limits the size of an included array; see
[Troubleshooting dialects](../dialects/troubleshooting-dialects.md).

## Related

- [Query reference](query-reference.md)
- [Troubleshooting queries](troubleshooting-queries.md)
- [Dialect reference](../dialects/dialect-reference.md)
