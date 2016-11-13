package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



/**
 * Interface shared by {@link Variable} and {@link IRI}. Used when a value can have one of those two types.
 */
public interface VarOrIri extends Node, Expression
{
}
