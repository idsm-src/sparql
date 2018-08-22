package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantIriMapping extends IriMapping implements ConstantMapping
{
    private final IRI value;


    public ConstantIriMapping(UserIriClass iriClass, IRI iri)
    {
        super(iriClass);
        value = iri;
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        return value.equals(node);
    }


    @Override
    public String getSqlValueAccess(int i)
    {
        return getIriClass().getInverseFunction(i) + "('" + value.getUri().toString() + "')";
    }


    @Override
    public NodeMapping remapColumns(List<KeyPair> columnMap)
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
}
