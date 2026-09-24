/**
 * Translation of the AST into intermediate code. {@link cz.iocb.sparql.engine.translator.TranslateVisitor} applies the
 * SPARQL algebra, the expression and path visitors translate expressions and property paths, and
 * {@link cz.iocb.sparql.engine.translator.VariableBindings} tracks the resource classes each variable may take and
 * generates the column expressions that unify them across joins.
 */
package cz.iocb.sparql.engine.translator;
