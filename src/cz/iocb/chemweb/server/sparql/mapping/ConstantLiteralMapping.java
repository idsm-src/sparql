package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class ConstantLiteralMapping extends ConstantMapping
{
    protected final ResourceClass resourceClass;
    protected final List<Column> columns;


    public ConstantLiteralMapping(LiteralClass literalClass, Literal value)
    {
        super(value);

        this.resourceClass = literalClass;
        this.columns = literalClass.toColumns(value);
    }


    @Override
    public ResourceClass getResourceClass()
    {
        return resourceClass;
    }


    @Override
    public List<Column> getColumns()
    {
        return columns;
    }
}
