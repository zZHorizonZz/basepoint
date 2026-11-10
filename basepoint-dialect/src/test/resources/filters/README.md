# Filter cases

Each file holds cases of filter text. A case is a description, a filter and its result, and a blank line ends it.

```
A string equals.
filter orders: state = "OPEN"
rows: 1, 2, 5
```

| Line | What it does |
| ---- | ------------ |
| `filter <table>: <text>` | The filter in the AIP-160 syntax, on a table of `fixture.sql`. |
| `cel <table>: <text>` | The filter in CEL. |
| `filter <table>(<column>): <text>` | Reads the result from `<column>` instead of `id`. |
| `rows: <value>, ...` | The rows the filter keeps, by their `id`, in ascending order. No values means no rows. |
| `error: <message>` | Building the query fails with an error that contains the message. |

`FilterTextContractTest` runs the cases with `rows` on every database, and `FilterErrorTest` the cases with `error`.
