package cz.iocb.chemweb.server.sparql.mapping.classes;

import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class ResourceClass
{
    public static final String nullTag = "null";

    protected final String name;


    protected ResourceClass(String name)
    {
        this.name = name;
    }


    public abstract int getPartsCount();

    public abstract String getSqlColumn(String var, int par);

    public abstract String getSparqlValue(String var);

    public abstract String getSqlValue(Node node, int i);


    public final String getName()
    {
        return name;
    }
}
