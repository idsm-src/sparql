package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.triple.Node;



/**
 * Interface shared by {@link VariableNode} and {@link IriNode}. Used when a value can have one of those two types.
 */
public interface VarOrIri extends Node, Expression
{
}
