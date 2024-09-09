package cz.iocb.sparql.engine.mapping;



public class ConstantBlankNodeMapping extends ConstantMapping
{
    public ConstantBlankNodeMapping(BlankNodeLiteral bnode)
    {
        super(bnode, bnode.getResourceClass(), bnode.getResourceClass().toColumns(bnode));
    }
}
