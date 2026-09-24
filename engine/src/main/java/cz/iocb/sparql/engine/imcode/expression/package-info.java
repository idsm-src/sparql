/**
 * Expression nodes of the intermediate code: operators, built-in and extension function calls, casts, constants and
 * variable references. An expression is evaluated separately for each combination of resource classes of its operands,
 * so that native SQL types are used wherever the classes are known and boxing to {@code sparql.rdfbox} is the fallback;
 * SPARQL errors are represented by SQL NULL.
 */
package cz.iocb.sparql.engine.imcode.expression;
