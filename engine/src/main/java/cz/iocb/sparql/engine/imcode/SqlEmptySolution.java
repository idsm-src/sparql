package cz.iocb.sparql.engine.imcode;

import java.util.Collection;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * The single empty solution (the identity of join); translates to {@code SELECT 1}.
 */
public final class SqlEmptySolution extends SqlIntercode
{
    /**
     * The only instance.
     */
    static private final SqlEmptySolution singleton = new SqlEmptySolution();


    /**
     * Creates the singleton.
     */
    private SqlEmptySolution()
    {
        super(new VariableBindings(), true);
    }


    /**
     * The only instance.
     *
     * @return the only instance
     */
    public static SqlEmptySolution get()
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
        return "SELECT 1";
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
        builder.append("empty solution");
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
