# Add a decorator

Rewrite statements before they render, to apply a rule to every statement, such as a tenant filter or a soft-delete
filter. Use a decorator rather than a filter in each query when no query may miss the rule. For background, see
[About statements and decorators](about-statements-and-decorators.md).

## Before you begin

Choose the node type that your rule rewrites. A rule about whole statements, such as a filter of a table, rewrites
`Statement`. A rule about one part, such as deletes, rewrites that clause, such as `DeleteFrom`.

## Add a filter to every query of a table

This example adds `tenant_id = 42` to every SELECT of the `orders` table.

1. Write a decorator of `Statement` nodes. A SELECT of a table has the source `Source.Table`, with the name of the
   table. Return other statements unchanged:

   ```java
   Decorator tenant = Decorator.of(Statement.class, statement -> {
     if (!(statement.source() instanceof Source.Table(String table) && table.equals("orders")
         && statement.has(Select.class))) {
       return statement;
     }
     return statement.where(eq("tenant_id", 42));
   });
   ```

   `where` joins the filter to the `WHERE` clause of the statement, or adds a `WHERE` clause after `FROM` and the
   joins if the statement has none.

2. Create a dialect that applies the decorator, and read the table from it:

   ```java
   Dialect dialect = new PostgresDialect().decorate(tenant);
   CollectionReference orders = dialect.collection("orders");
   ```

3. Build queries as usual:

   ```java
   orders.query().where(eq("state", "OPEN"));
   ```

The query renders:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."state" = $1) AND ("t0"."tenant_id" = $2))
```

The tenant filter is in the SQL, though the query did not add it.

## Decorate one node type

`Decorator.of(type, rewrite)` applies `rewrite` only to nodes of `type`. Other nodes pass through unchanged. For
example, this decorator rejects every `DELETE`:

```java
Decorator noDeletes = Decorator.of(DeleteFrom.class, delete -> {
  throw new UnsupportedOperationException("Deletes are not allowed.");
});
```

## Combine decorators

Use `then` to apply decorators in order:

```java
Dialect dialect = new PostgresDialect().decorate(tenant.then(noDeletes));
```

Your decorators run before the dialect's decorator. They see standard clauses, such as `Limit` and
`Returning`, before the dialect rewrites them.

## Rules for decorators

- The writer calls the decorator once for each node, as the node renders.
- The writer renders the returned node without decorating it again.
- Statements are immutable. Return a new statement from `with`, `replace`, `before`, `after`, `remove`, `put`,
  `add` or `where`.
- A decorator applies to subqueries too, such as the queries of `include` and `exists`.

## Related

- [About statements and decorators](about-statements-and-decorators.md)
- [Dialect reference](dialect-reference.md)
