package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.BlankNode;



/**
 * Mapping of a position to a fixed blank node, represented by the constant columns of its blank node class.
 */
public class ConstantBlankNodeMapping extends ConstantMapping
{
    /**
     * Creates the mapping; the columns are the constant columns of the class for the node.
     *
     * @param bnode the blank node
     * @param bnodeClass the resource class
     */
    public ConstantBlankNodeMapping(BlankNode bnode, ResourceClass bnodeClass)
    {
        super(bnode, bnodeClass, bnodeClass.toColumns(null, bnode));
    }
}
