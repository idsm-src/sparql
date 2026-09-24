package cz.iocb.sparql.engine.model.base;

import cz.iocb.sparql.engine.model.triple.ComplexNode;



/**
 * Common base class for {@link ComplexNode} implementations.
 */
public abstract class BaseComplexNode extends BaseElement implements ComplexNode
{
    /**
     * Creates the node.
     */
    protected BaseComplexNode()
    {
    }
}
