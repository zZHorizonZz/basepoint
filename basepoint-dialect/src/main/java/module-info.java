module dev.horizon.basepoint {
  requires transitive dev.horizon.aquery.api;
  requires dev.horizon.aquery.ebnf;

  exports dev.horizon.basepoint;
  exports dev.horizon.basepoint.expression;
  exports dev.horizon.basepoint.filter;
  exports dev.horizon.basepoint.statement;
}
