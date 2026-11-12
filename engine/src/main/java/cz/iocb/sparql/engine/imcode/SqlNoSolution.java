package cz.iocb.sparql.engine.imcode;

import java.util.Collection;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * No solution at all (the identity of union and the zero of join).
 */
public final class SqlNoSolution extends SqlIntercode
{
    /**
     * The only instance.
     */
    static private final SqlNoSolution singleton = new SqlNoSolution();

    /**
     * SQL code of the node.
     */
    static private final SqlCode code = new SqlCode("SELECT 1 WHERE false");


    /**
     * Creates the singleton.
     */
    private SqlNoSolution()
    {
        super(new VariableBindings(), true);
    }


    /**
     * The only instance.
     *
     * @return the only instance
     */
    public static SqlNoSolution get()
    {
        return singleton;
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        return this;
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return code;
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
        return SqlNoSolution.class.getName().hashCode();
    }
}
