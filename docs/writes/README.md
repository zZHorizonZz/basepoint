# Writes

An `InsertQuery`, `UpdateQuery` or `DeleteQuery` writes rows of one table. An insert can upsert rows whose key
exists, and every write can return the rows it wrote. An update or delete needs a filter, or `all()` to write every
row, so that a forgotten filter does not write the whole table.

## Features

**Inserts and upserts.** Insert one or several rows, the default values of every column, or skip or update the rows
whose key exists. See [Insert rows](insert-rows.md).

**Updates.** Set, increment and unset columns of the rows that a filter keeps. See [Update rows](update-rows.md).

**Deletes.** Delete the rows that a filter keeps. See [Delete rows](delete-rows.md).

Every method of the writes is in [Write reference](write-reference.md). The errors of writes are in
[Troubleshooting writes](troubleshooting-writes.md).

## Related

- [Filters](../filters/README.md)
- [Dialects](../dialects/README.md)
