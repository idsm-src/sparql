package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.rdf.BlankNode;



public class ConstantBlankNodeMapping extends ConstantMapping
{
    public ConstantBlankNodeMapping(BlankNode bnode, BlankNodeClass bnodeClass)
    {
        super(bnode, bnodeClass, bnodeClass.toColumns(bnode));
    }
}
