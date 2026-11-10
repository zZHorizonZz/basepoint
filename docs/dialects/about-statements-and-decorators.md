# About statements and decorators

A builder does not write SQL text. It holds a statement tree and changes it with each call. A dialect then renders
the tree, and decorators can rewrite parts of the tree before they render.

## Packages

| Package                            | Contents                                                                                     |
| ---------------------------------- | -------------------------------------------------------------------------------------------- |
| `dev.horizon.basepoint`            | `Dialect`, `CollectionReference`, the builders `Query`, `InsertQuery`, `UpdateQuery` and `DeleteQuery`, and the exceptions |
| `dev.horizon.basepoint.expression` | `Expression`, its factories such as `column` and `sum`, and a record for each kind of value  |
| `dev.horizon.basepoint.filter`     | `Filter`, its factories such as `eq` and `exists`, and a record for each kind of filter      |
| `dev.horizon.basepoint.statement`  | `Statement`, `Source`, the clauses, `Include`, `StatementWriter` and `Decorator`             |

You build statements with the first three packages. Dialects and decorators work with the fourth.

## Nodes

Every part of a statement is a `Node`. A node has one method, `render(StatementWriter)`, which writes the node as
standard SQL. These types are nodes:

| Type         | Examples                                                                                        |
| ------------ | ----------------------------------------------------------------------------------------------- |
| `Statement`  | A SELECT, INSERT, UPDATE or DELETE, or queries combined with UNION                              |
| `Clause`     | `Select`, `From`, `Join`, `Where`, `OrderBy`, `Limit`, `InsertInto`, `Assignments`, `Returning` |
| `Expression` | `Column`, `Value`, `Aggregate`, `Subquery`, `Include.Many`, `JsonKey`                           |
| `Filter`     | `Compare`, `In`, `And`, `Exists`, `Text`                                                        |

## Statements

A `Statement` is a `Source` and a list of clauses. The clauses define the kind of statement. There are no separate
types for SELECT, INSERT, UPDATE and DELETE.

For example, `orders.update().set("state", "CLOSED").where(eq("id", 1))` holds this statement:

| Part    | Value                            |
| ------- | -------------------------------- |
| Source  | `Source.Target("orders")`        |
| Clauses | `Update`, `Assignments`, `Where` |

The source decides how columns are qualified:

| Source          | Rows                                                   | A column renders as |
| --------------- | ------------------------------------------------------ | ------------------- |
| `Source.Table`  | A table that a SELECT reads, under a generated alias   | `"t0"."id"`         |
| `Source.Rows`   | The rows of another statement, under a generated alias | `"t0"."id"`         |
| `Source.Target` | The table that an INSERT, UPDATE or DELETE writes      | `"orders"."id"`     |
| `Source.None`   | No rows of its own, as in a UNION                      |                     |

Statements are immutable. Methods such as `with`, `replace`, `before`, `after`, `remove`, `put`, `add` and `where`
return a new statement. `where` narrows a SELECT, an UPDATE or a DELETE by a filter, whether or not it has a `WHERE`
clause. Call `statement()` on any builder to get its tree before it renders.

Filter text stays a `Text` filter in the tree until it renders. It translates then, with the relations of the
statement and of the statements around it, so `customer.region` reads a relation whatever the order of the calls.

## Rendering

`build()` calls `dialect.render(statement())`, which renders the statement with a `StatementWriter`. The writer:

- Quotes identifiers with the dialect's quote characters.
- Writes each value as a placeholder in the dialect's style and adds the value to the parameter list.
- Tracks the source and the relations of each statement, so that the columns of subqueries and their enclosing
  queries get the right qualifier.
- Passes every node to the dialect's decorator before rendering it.

## Decorators

A `Decorator` receives a node and returns the node to render. It can return the same node, a changed copy, or a
different node.

The writer calls the decorator for the statement first, and then for each child node as the child renders. It
renders the returned node without decorating it again.

Each dialect has one decorator, combined from smaller ones with `then`. For example, the SQL Server dialect uses
these decorators:

| Decorator         | Rewrite                                                               |
| ----------------- | --------------------------------------------------------------------- |
| `SqlServerLock`   | `Lock` becomes a table hint, such as `WITH (UPDLOCK, ROWLOCK)`.       |
| `SqlServerMerge`  | An insert with `OnConflict` becomes a `MERGE` statement.              |
| `SqlServerOutput` | `Returning` becomes `OUTPUT` at the position that SQL Server expects. |
| `SqlServerPaging` | `Limit` becomes `TOP`. `Offset` becomes `OFFSET ... FETCH`.           |
| `SqlServerObject` | `Include.One` becomes `FOR JSON PATH, WITHOUT_ARRAY_WRAPPER`.         |
| `SqlServerArray`  | `Include.Many` becomes `FOR JSON PATH`.                               |

A new clause type renders as standard SQL on every dialect. A dialect needs a decorator only when its database
writes that clause differently. Standard SQL has no syntax for an include, so a dialect without decorators of
`Include.One` and `Include.Many` throws `UnsupportedFeatureException` when a query with an include renders.

Applications can add decorators too. `dialect.decorate(decorator)` returns a dialect that applies your decorator
before the dialect's own. For an example, see [Add a decorator](add-a-decorator.md).

## Related

- [Dialect reference](dialect-reference.md)
- [Add a decorator](add-a-decorator.md)
