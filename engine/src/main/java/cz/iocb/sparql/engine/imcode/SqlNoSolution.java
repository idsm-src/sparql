package cz.iocb.sparql.engine.imcode;

import java.util.Collection;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlNoSolution extends SqlIntercode
{
    static private final SqlNoSolution singleton = new SqlNoSolution();


    private SqlNoSolution()
    {
        super(new VariableBindings(), true);
    }


    public static SqlNoSolution get()
    {
        return singleton;
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        return this;
    }


    @Override
    public String translate(Request request)
    {
        return "SELECT 1 WHERE false";
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return true;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return false;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("no solution");
    }


    @Override
    public boolean equals(Object object)
    {
        return object == singleton;
    }


    @Override
    protected int getHashCode()
    {
        return System.identityHashCode(singleton);
    }
}
