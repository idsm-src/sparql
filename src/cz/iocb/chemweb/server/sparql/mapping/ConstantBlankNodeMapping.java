package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class ConstantBlankNodeMapping extends ConstantMapping
{
    public ConstantBlankNodeMapping(BlankNodeLiteral blankNodeLiteral)
    {
        super(blankNodeLiteral);
    }


    @Override
    public ResourceClass getResourceClass()
    {
        return ((BlankNodeLiteral) value).getResourceClass();
    }


    @Override
    public List<Column> getColumns()
    {
        return ((BlankNodeLiteral) value).getResourceClass().toColumns(value);
    }
}
