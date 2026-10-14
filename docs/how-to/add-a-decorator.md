# Add a decorator

A decorator rewrites statements before they render. Use a decorator to apply a rule to every statement, for example
a tenant filter or a soft-delete filter. For background, see
[Statements and decorators](../concepts/statements-and-decorators.md).

## Add a filter to every query of a table

This example adds `tenant_id = 42` to every SELECT on the `orders` table.

1. Write a decorator for `Statement` nodes:

   ```java
   Decorator tenant = Decorator.of(Statement.class, statement -> {
     if (!(statement.source() != null && statement.source().rows() instanceof Source.Table table
         && table.name().equals("orders") && statement.has(Clause.Select.class))) {
       return statement;
     }
     Filter filter = eq("tenant_id", 42);
     return statement.find(Clause.Where.class)
         .map(where -> statement.replace(where, new Clause.Where(and(where.filter(), filter))))
         .orElseGet(() -> statement.after(Clause.From.class, new Clause.Where(filter)));
   });
   ```

   The decorator returns other statements unchanged. If the statement has a `WHERE` clause, the decorator combines
   the filters. Otherwise, it adds a `WHERE` clause after `FROM`.

2. Create a dialect that applies the decorator:

   ```java
   Dialect dialect = new PostgresDialect().decorated(tenant);
   CollectionReference orders = dialect.collection("orders");
   ```

3. Build queries as usual:

   ```java
   orders.query().where(eq("state", "OPEN")).build();
   ```

   This renders:

   ```sql
   SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."state" = $1) AND ("t0"."tenant_id" = $2))
   ```

## Decorate one node type

`Decorator.of(type, rewrite)` applies `rewrite` only to nodes of `type`. Other nodes pass through unchanged. For
example, this decorator rejects every `DELETE`:

```java
Decorator noDeletes = Decorator.of(Clause.DeleteFrom.class, delete -> {
  throw new UnsupportedOperationException("Deletes are not allowed.");
});
```

## Combine decorators

Use `then` to apply decorators in order:

```java
Dialect dialect = new PostgresDialect().decorated(tenant.then(noDeletes));
```

Your decorators run before the dialect's decorator. They see standard clauses, such as `Clause.Limit` and
`Clause.Returning`, before the dialect rewrites them.

## Rules for decorators

- The writer calls the decorator once for each node, as the node renders.
- The writer renders the returned node without decorating it again.
- Statements are immutable. Return a new statement from `with`, `replace`, `before`, `after` or `remove`.
- A decorator applies to subqueries too, such as the queries of `include` and `exists`.

## Related pages

- [Statements and decorators](../concepts/statements-and-decorators.md)
- [Builders](../reference/builders.md)
