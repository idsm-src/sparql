/**
 * Intermediate code: a relational-algebra tree of {@link cz.iocb.sparql.engine.imcode.SqlIntercode} nodes built by the
 * translator from the SPARQL AST. Each node tracks the resource classes its variables may take, rewrites itself for
 * what its parent needs in {@code optimize} and emits an SQL subquery in {@code translate}.
 * {@link cz.iocb.sparql.engine.imcode.SqlSelect} is the root of a translated query.
 */
package cz.iocb.sparql.engine.imcode;
