package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantIriMapping extends IriMapping
{
    private final String value;


    public ConstantIriMapping(IriClass iriClass, String iri)
    {
        super(iriClass);
        value = iri;
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof Variable)
            return true;

        if(node instanceof IRI && value.equals(((IRI) node).getUri().toString()))
            return true;

        return false;
    }


    @Override
    public String getSqlValueAccess(int i)
    {
        return getIriClass().getInverseFunction(i) + "('" + value + "')";
    }
}
