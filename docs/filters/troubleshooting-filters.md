# Troubleshooting filters

The errors of filters and filter text, the most frequent first. Filter text that does not parse fails when you pass
it to `where` or `Filter.of`. Filter text that parses but has no translation fails in `build()`.

## The filter term 'plain' has no column. Compare a column with a value...

```text
dev.horizon.aquery.aip160.InvalidFilterException: The filter term 'plain' has no column. Compare a column with a value, for example state = OPEN.
```

**Cause:** A word of the text that compares nothing. It is a search term in AIP-160, which Basepoint does not
translate. The text `state = OPEN AND` reports `AND` this way, and `total ~ 5` reports `total`, since `~` is no
operator.

**Fix:** Compare a column with a value, such as `state = OPEN`. Check the operator and the words around the term in
the message.

**Verify:** `build()` returns the statement, and its `WHERE` clause compares the column.

## Unsupported field path 'a.b.c'. Use a column name, a column and a key...

```text
dev.horizon.aquery.aip160.InvalidFilterException: Unsupported field path 'a.b.c'. Use a column name, a column and a key of the JSON object in it, such as labels.env, or a relation and its column, such as customer.region.
```

**Cause:** A field of three or more parts. A field is a column, a column and a key of its JSON object, or a relation
and its column.

**Fix:** Use a field of one or two parts. For a key inside a nested JSON object, use a Java filter of your own.

**Verify:** `build()` returns the statement.

## Unsupported operator '>' on a key of a JSON object, whose values...

```text
dev.horizon.aquery.aip160.InvalidFilterException: Unsupported operator '>' on a key of a JSON object, whose values compare only as text.
```

**Cause:** `<`, `<=`, `>` or `>=` on a key of a JSON object, such as `labels.env > 1`. The values of keys compare as
text, and text does not order like numbers.

**Fix:** Compare the key with `=`, `!=` or a pattern in quotes. To compare a number, store it in a column of its own.

**Verify:** `build()` returns the statement, and its `WHERE` clause compares the key with `=`.

## Unsupported filter 'x'. A key of a JSON object supports only ':*'...

```text
dev.horizon.aquery.aip160.InvalidFilterException: Unsupported filter 'x'. A key of a JSON object supports only ':*', which matches rows whose object has the key.
```

**Cause:** `labels.env:x`. The has operator on a key tests only whether the object has the key.

**Fix:** Write `labels.env:*` or `labels:env` to test for the key, or `labels.env = x` to compare its value.

**Verify:** The SQL tests the key with `IS NOT NULL`.

## Unsupported filter 'order.region:EU'. order is a relation of the...

```text
dev.horizon.aquery.aip160.InvalidFilterException: Unsupported filter 'order.region:EU'. order is a relation of the query, and a column of a relation supports only ':*', which matches the rows whose related row has a value in it.
```

**Cause:** The has operator with a value on a column of a relation.

**Fix:** Write `order.region:*` to test for a value, or `order.region = EU` to compare it.

**Verify:** The SQL compares `"t1"."region"`.

## Cannot compare null with >. Use = or != with null.

```text
dev.horizon.aquery.aip160.InvalidFilterException: Cannot compare null with >. Use = or != with null.
```

**Cause:** `null` with `<`, `<=`, `>` or `>=`, such as `total > null`. Such a comparison matches no row in SQL.

**Fix:** Write `total = null` or `total != null` to test for `NULL`.

**Verify:** The SQL has `IS NULL` or `IS NOT NULL`.

## unterminated string starting at position 8

```text
dev.horizon.aquery.aip160.InvalidFilterException: unterminated string starting at position 8
```

**Cause:** A quoted value without its closing quote, such as `state = "OPEN`. The position counts from 0.

**Fix:** Close the quote. Write a quote inside a value as `\"`.

**Verify:** `where(text)` returns the query.

## expected RPAREN but got END()

```text
dev.horizon.aquery.aip160.InvalidFilterException: expected RPAREN but got END()
```

**Cause:** A group without its closing parenthesis, such as `(state = OPEN`. `expected END but got RPAREN()` is the
other way round: a closing parenthesis without its group.

**Fix:** Close each group that you open.

**Verify:** `where(text)` returns the query.

## expected arg after >

```text
dev.horizon.aquery.aip160.InvalidFilterException: expected arg after >
```

**Cause:** An operator without a value, such as `total >`.

**Fix:** Write the value after the operator, such as `total > 100`.

**Verify:** `where(text)` returns the query.

## parentheses nest more than 64 levels deep

```text
dev.horizon.aquery.aip160.InvalidFilterException: parentheses nest more than 64 levels deep
```

**Cause:** Filter text with more than 64 levels of parentheses, the most that the aquery parser reads.

**Fix:** Remove the parentheses that group nothing. Text that a program writes can join terms at one level, such as
`id = 1 OR id = 2 OR id = 3`, or use a Java filter.

**Verify:** `where(text)` returns the query.

## A JSON key must not be empty or contain " or \, but is a"b.

```text
java.lang.IllegalArgumentException: A JSON key must not be empty or contain " or \, but is a"b.
```

**Cause:** `key(column, key)` with an empty key, or a key with `"` or `\`. In filter text, the same key throws
`InvalidFilterException` with `Unsupported key`.

**Fix:** Use keys without `"` and `\`. A key can have dots, slashes and spaces, such as `app.kubernetes.io/name`.

**Verify:** The SQL binds the path `$."<key>"` as a placeholder.

## Related

- [Filter rows](filter-rows.md)
- [Filter text syntax](filter-text-syntax.md)
- [Exceptions](../exceptions.md)
