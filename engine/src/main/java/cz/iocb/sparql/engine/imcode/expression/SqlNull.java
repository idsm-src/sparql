package cz.iocb.sparql.engine.imcode.expression;

import java.util.Map;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlNull extends SqlExpressionIntercode
{
    static private final SqlNull singleton = new SqlNull();


    private SqlNull()
    {
        super(Map.of(), true, true);
    }


    public static SqlExpressionIntercode get()
    {
        return singleton;
    }


    @Override
    public Restrictions getRequirements()
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        return this;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("null");
    }


    @Override
    public boolean equals(Object object)
    {
        return object == this;
    }


    @Override
    protected int getHashCode()
    {
        return System.identityHashCode(this);
    }
}
