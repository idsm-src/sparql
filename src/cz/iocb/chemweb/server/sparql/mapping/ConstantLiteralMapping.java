package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantLiteralMapping extends LiteralMapping implements ConstantMapping
{
    private final Literal value;


    public ConstantLiteralMapping(LiteralClass literalClass, Literal value)
    {
        super(literalClass);
        this.value = value;

        if(value == null)
            throw new RuntimeException();
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        return value.equals(node);
    }


    @Override
    public String getSqlValueAccess(int part)
    {
        return getResourceClass().getPatternCode(value, part);
    }


    @Override
    public NodeMapping remapColumns(List<ColumnPair> columnMap)
    {
        return this;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ConstantLiteralMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ConstantLiteralMapping other = (ConstantLiteralMapping) obj;

        return value.equals(other.value);
    }


    @Override
    public Node getValue()
    {
        return value;
    }
}
