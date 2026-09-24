/**
 * Parsing of SPARQL queries: the ANTLR-generated parser is driven by {@link cz.iocb.sparql.engine.parser.Parser} and
 * the parse tree is converted into the AST by a set of visitors, which also resolve prefixed names, check variable
 * scopes and report syntax errors.
 */
package cz.iocb.sparql.engine.parser;
