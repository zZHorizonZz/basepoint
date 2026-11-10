# Basepoint

Basepoint builds SQL statements for PostgreSQL, MariaDB, SQL Server and SQLite. You describe a query, insert, update
or delete with immutable builders, and Basepoint gives the SQL text of your database and the values of its
placeholders. You run the statement with any client, such as JDBC, R2DBC or the Vert.x SQL client. Basepoint does
not connect to a database and does not need a schema.

To build your first query, see [Get started](get-started.md).

## Areas

| Area                             | What you do there                                                                                   |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| [Queries](queries/README.md)     | Read rows: select, group and combine them, include related rows, compute values, use WITH queries.  |
| [Filters](filters/README.md)     | Choose the rows that a query, update or delete affects, in Java or as filter text.                  |
| [Writes](writes/README.md)       | Insert, upsert, update and delete rows, and return the rows you wrote.                              |
| [Dialects](dialects/README.md)   | Render for each database, and rewrite statements with decorators.                                   |

The exceptions of all areas are in [Exceptions](exceptions.md).
