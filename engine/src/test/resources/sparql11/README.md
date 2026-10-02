# SPARQL 1.1 query test suites

Copy of the query suites of the W3C/community SPARQL test suite, https://github.com/w3c/rdf-tests, directory
`sparql/sparql11`: the suites listed in its `manifest-sparql11-query.ttl`. Taken from branch `main` at commit
`ca72fa9` (2026-10-01).

SPARQL does not define the lexical form of a literal computed by an expression (arithmetic, casts, aggregates,
rounding, date functions): the expected files carry the forms of the implementation that produced them. The engine
is compared with the expected results term by term, so the following files deviate from upstream where a computed
value is formatted differently by the engine (the values are the same):

- `aggregates/agg-avg-distinct.srx`, `aggregates/agg-sum-distinct.srx`: doubles as `1.05E3`, `2.1E3`.
- `aggregates/agg-min-02.srx`: `MIN` returns the data term `2E-1`, upstream writes `2.0E-1`.
- `cast/cast-decimal.srx`, `cast/cast-double.srx`, `cast/cast-float.srx`: the cast results as `0.0`, `0.0E0`,
  `1.3E1`, ...; in `cast-decimal.srx` the `?v` bindings of `:n07`-`:n10` are the data terms `0E1` and `1E0`, which
  upstream wrongly writes as `0.0` and `1.0`.
- `functions/ceil01.srx`, `functions/floor01.srx`, `functions/round01.srx`, `functions/seconds-01.srx`: integral
  decimals as `3.0`, upstream writes `3`.

To update from upstream, copy the suite directories over this one, delete the files no longer referenced by the
manifests, and re-apply the deviations above (`git diff` against the previous commit shows them); then run
`SparqlTest` and treat every remaining difference in the lexical form of a computed value the same way.
