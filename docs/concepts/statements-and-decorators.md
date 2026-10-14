# Statements and decorators

A builder does not write SQL text directly. It creates a statement tree. A dialect then renders the tree, and
decorators can rewrite parts of the tree before they render.

## Nodes

Every part of a statement is a `Node`. A node has one method, `render(StatementWriter)`, which writes the node as
standard SQL. These types are nodes:

| Type         | Examples                                                                           |
| ------------ | ---------------------------------------------------------------------------------- |
| `Statement`  | A SELECT, INSERT, UPDATE or DELETE                                                 |
| `Clause`     | `Select`, `From`, `Where`, `OrderBy`, `Limit`, `InsertInto`, `Values`, `Returning` |
| `Expression` | `Column`, `Value`, `Aggregate`, `Subquery`, `Include`                              |
| `Filter`     | `Compare`, `In`, `And`, `Exists`                                                   |

## Statements

A `Statement` is a `Source` and a list of clauses. The clauses define the kind of statement. There are no separate
types for SELECT, INSERT, UPDATE and DELETE.

For example, `orders.update().set("state", "CLOSED").where(eq("id", 1))` creates this statement:

| Part    | Value                                       |
| ------- | ------------------------------------------- |
| Source  | The table `orders`, named by its table name |
| Clauses | `Update`, `Set`, `Where`                    |

The source decides how columns are qualified. SELECT statements use a generated alias, such as `"t0"`. INSERT,
UPDATE and DELETE statements use the table name.

Statements are immutable. Methods such as `with`, `replace`, `before`, `after` and `remove` return a new statement.
Call `statement()` on any builder to get its tree before it renders.

## Rendering

`build()` creates a `StatementWriter` for the dialect and renders the statement. The writer:

- Quotes identifiers with the dialect's quote characters.
- Writes each value as a placeholder in the dialect's style and adds the value to the parameter list.
- Tracks the current source, so that columns of subqueries and their enclosing queries get the right qualifier.
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
| `SqlServerMerge`  | An insert with `OnConflict` becomes a `MERGE` statement.              |
| `SqlServerOutput` | `Returning` becomes `OUTPUT` at the position that SQL Server expects. |
| `SqlServerPaging` | `Limit` becomes `TOP`. `Offset` becomes `OFFSET ... FETCH`.           |
| `SqlServerJson`   | `Include` becomes `FOR JSON PATH`.                                    |

A new clause type renders as standard SQL on every dialect. A dialect needs a decorator only when its database
writes that clause differently.

Applications can add decorators too. `dialect.decorated(decorator)` returns a dialect that applies your decorator
before the dialect's own. For an example, see [Add a decorator](../how-to/add-a-decorator.md).

## Related pages

- [Dialects](../reference/dialects.md)
- [Add a decorator](../how-to/add-a-decorator.md)
