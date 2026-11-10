# Dialects

A `Dialect` renders statements for one database: PostgreSQL, MariaDB, SQL Server or SQLite. It quotes names,
writes placeholders in the style of your client, and rewrites the parts of a statement that its database writes
differently, such as limits, upserts and JSON. You can add your own rewrites, called decorators.

## Features

**Statements and decorators.** Builders hold a tree of nodes, and a dialect renders the tree after its decorators
rewrite it. See [About statements and decorators](about-statements-and-decorators.md).

**Your own decorators.** Apply a rule to every statement, such as a tenant filter. See
[Add a decorator](add-a-decorator.md).

**The SQL of each database.** Quotes, placeholders, and every difference between the databases are in
[Dialect reference](dialect-reference.md). The errors and surprises of each database are in
[Troubleshooting dialects](troubleshooting-dialects.md).

## Related

- [Queries](../queries/README.md)
- [Writes](../writes/README.md)
