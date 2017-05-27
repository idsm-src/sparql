package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class IriClass extends ResourceClass
{
    public static final String iriTag = "iri";

    private final String pattern;
    private final HashSet<String> values;
    private final String function;
    private final List<String> inverseFunction;
    private final List<String> sqlTypes;


    public IriClass(String name, List<String> sqlTypes, String pattern, HashSet<String> values)
    {
        super(name);
        this.pattern = pattern;
        this.values = values;
        this.function = name;
        this.sqlTypes = sqlTypes;

        this.inverseFunction = new ArrayList<String>(sqlTypes.size());

        if(sqlTypes.size() == 1)
            this.inverseFunction.add(name + "_inverse");
        else
            for(int i = 0; i < sqlTypes.size(); i++)
                this.inverseFunction.add(name + "_inv" + (i + 1));
    }


    public IriClass(String name, List<String> types, String pattern)
    {
        this(name, types, pattern, null);
    }


    public IriClass(String name, List<String> types, HashSet<String> values)
    {
        this(name, types, null, values);
    }


    @Override
    public int getPartsCount()
    {
        return inverseFunction.size();
    }


    @Override
    public String getSqlColumn(String var, int par)
    {
        if(inverseFunction.size() > 1)
            return '"' + var + '#' + name + "-par" + par + '"';
        else
            return '"' + var + '#' + name + '"';
    }


    @Override
    public String getSparqlValue(String var)
    {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < inverseFunction.size(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(getSqlColumn(var, i));
        }

        strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public String getSqlValue(Node node, int i)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), i);

        if(!(node instanceof IRI))
            return null;

        IRI iri = (IRI) node;

        return inverseFunction.get(i) + "('" + iri.getUri().toString() + "')";
    }


    @Override
    public String getSqlType(int i)
    {
        return sqlTypes.get(i);
    }


    @Override
    public boolean match(Node value)
    {
        if(!(value instanceof IRI))
            return false;

        return match(((IRI) value).getUri().toString());
    }


    public boolean match(String iri)
    {
        if(values != null && values.contains(iri))
            return true;

        if(pattern != null && iri.matches(pattern))
            return true;

        return false;
    }


    public final String getFunction()
    {
        return function;
    }


    public String getInverseFunction(int i)
    {
        return inverseFunction.get(i);
    }
}
