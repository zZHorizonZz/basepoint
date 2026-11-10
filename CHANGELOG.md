# Changelog

## [0.4.0](https://github.com/zZHorizonZz/basepoint/compare/v0.3.1...v0.4.0) (2026-11-10)


### ⚠ BREAKING CHANGES

* rebuild basepoint as final builders over a statement tree, with tests on four databases and docs by area. ([#34](https://github.com/zZHorizonZz/basepoint/issues/34))
* Expression.Plus is replaced by Expression.Arithmetic, which has the operators ADD, SUBTRACT, MULTIPLY and DIVIDE.
* Query.includeCount is removed. Use select(name, subquery(rows.count())) in its place.

### Features

* add expression functions that each database writes in its own way. ([#28](https://github.com/zZHorizonZz/basepoint/issues/28)) ([884b167](https://github.com/zZHorizonZz/basepoint/commit/884b1672c9df2719afb6e015e2ed8f6abb253fb1))
* add recursive WITH queries, whose step reads them through a relation. ([#32](https://github.com/zZHorizonZz/basepoint/issues/32)) ([eed8aca](https://github.com/zZHorizonZz/basepoint/commit/eed8acab5719e6cc60cc374af9a96dc2c30afa4c))
* add WITH queries, which a query reads like tables. ([#30](https://github.com/zZHorizonZz/basepoint/issues/30)) ([d48ed73](https://github.com/zZHorizonZz/basepoint/commit/d48ed7308cf01932d74b9b94ebf953cd484bfdb9))
* move a timestamp by a duration, with add and subtract. ([#29](https://github.com/zZHorizonZz/basepoint/issues/29)) ([b73b7a8](https://github.com/zZHorizonZz/basepoint/commit/b73b7a819e84170fcd76fe05544355b84a6ca7bf))
* name the related row of each row with relation, and read its columns through paths. ([#31](https://github.com/zZHorizonZz/basepoint/issues/31)) ([e4ca6a4](https://github.com/zZHorizonZz/basepoint/commit/e4ca6a43f77f5474be08a02b4abc34b24a9fbdcc))
* read a column of a relation in filter text, such as customer.region. ([#33](https://github.com/zZHorizonZz/basepoint/issues/33)) ([5334c50](https://github.com/zZHorizonZz/basepoint/commit/5334c50a13ae58e3558f0f096057e723f2657769))
* use a query that gives one value as a value, with Expression.subquery. ([#26](https://github.com/zZHorizonZz/basepoint/issues/26)) ([dabb6a7](https://github.com/zZHorizonZz/basepoint/commit/dabb6a70f6ade0dd228440fb00e17b0be99f566f))


### Refactoring

* rebuild basepoint as final builders over a statement tree, with tests on four databases and docs by area. ([#34](https://github.com/zZHorizonZz/basepoint/issues/34)) ([59fa87f](https://github.com/zZHorizonZz/basepoint/commit/59fa87f836ad9ee6fcac3c2588aca6e61979719c))

## [0.3.1](https://github.com/zZHorizonZz/basepoint/compare/v0.3.0...v0.3.1) (2026-10-25)


### Fixes

* depend on aquery 0.3.0, not a snapshot of it. ([13ccbf8](https://github.com/zZHorizonZz/basepoint/commit/13ccbf8100d854eeff1e34c46b59d30caeba0eeb))

## [0.3.0](https://github.com/zZHorizonZz/basepoint/compare/v0.2.0...v0.3.0) (2026-10-21)


### Features

* filter by a key of a JSON column in AIP-160 text. ([fcd5620](https://github.com/zZHorizonZz/basepoint/commit/fcd5620aad5798d3e46c3c35bae58815666b3aec))

## [0.2.0](https://github.com/zZHorizonZz/basepoint/compare/v0.1.0...v0.2.0) (2026-10-21)


### Features

* filter and select by the value of a key of a JSON column. ([f056f17](https://github.com/zZHorizonZz/basepoint/commit/f056f1750409d5b5ed38d54c9f8ecb915a79aa07))
* lock the rows of a query, and narrow any statement with where. ([b6c239d](https://github.com/zZHorizonZz/basepoint/commit/b6c239ddf0c4358444080f524c1d29713c37e445))

## 0.1.0 (2026-10-14)


### Features

* a lightweight SQL builder for PostgreSQL, MariaDB, SQL Server and SQLite ([a2fae54](https://github.com/zZHorizonZz/basepoint/commit/a2fae54834a8e35df71b7a692c54ad720f05921c))
