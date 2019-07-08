package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantIriMapping extends IriMapping implements ConstantMapping
{
    private final IRI value;


    public ConstantIriMapping(IriClass iriClass, IRI iri)
    {
        super(iriClass);
        value = iri;
    }


    @Override
    public boolean match(Node node, Request request)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        return value.equals(node);
    }


    @Override
    public String getSqlValueAccess(int part)
    {
        return getIriClass().getPatternCode(value, part);
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

        if(obj == null || !(obj instanceof ConstantIriMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ConstantIriMapping other = (ConstantIriMapping) obj;

        return value.equals(other.value);
    }


    @Override
    public Node getValue()
    {
        return value;
    }


    @Override
    public String getSqlIriValueAccess()
    {
        return "'" + value.getValue() + "'::varchar";
    }
}
