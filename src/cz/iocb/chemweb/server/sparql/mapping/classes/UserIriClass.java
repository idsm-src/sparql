package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UserIriClass extends IriClass
{
    private final String pattern;
    private final HashSet<String> values;
    private final String function;
    private final List<String> inverseFunction;


    public UserIriClass(String name, List<String> sqlTypes, String pattern, HashSet<String> values)
    {
        super(name, sqlTypes, Arrays.asList(ResultTag.IRI));
        this.pattern = pattern;
        this.values = values;
        this.function = name;

        this.inverseFunction = new ArrayList<String>(sqlTypes.size());

        if(sqlTypes.size() == 1)
            this.inverseFunction.add(name + "_inverse");
        else
            for(int i = 0; i < sqlTypes.size(); i++)
                this.inverseFunction.add(name + "_inv" + (i + 1));
    }


    public UserIriClass(String name, List<String> types, String pattern)
    {
        this(name, types, pattern, null);
    }


    public UserIriClass(String name, List<String> types, HashSet<String> values)
    {
        this(name, types, null, values);
    }


    @Override
    public String getResultValue(String var, int part)
    {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(getSqlColumn(var, i));
        }

        strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public String getSqlValue(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), part);

        IRI iri = (IRI) node;

        return inverseFunction.get(part) + "('" + iri.getUri().toString() + "'::varchar)";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.iri;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        StringBuffer strBuf = new StringBuffer();

        if(rdfbox)
            strBuf.append("sparql.cast_as_rdfbox_from_iri(");

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(variableAccessor.variableAccess(variable, this, i));
        }

        strBuf.append(")");

        if(rdfbox)
            strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match(((IRI) node).getUri().toString());
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


    public String getInverseFunction(int part)
    {
        return inverseFunction.get(part);
    }
}
