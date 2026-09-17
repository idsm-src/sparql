package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.BlankNode;



public class ConstantBlankNodeMapping extends ConstantMapping
{
    public ConstantBlankNodeMapping(BlankNode bnode, ResourceClass bnodeClass)
    {
        super(bnode, bnodeClass, bnodeClass.toColumns(null, bnode));
    }
}
