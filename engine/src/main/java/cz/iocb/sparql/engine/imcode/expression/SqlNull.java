package cz.iocb.sparql.engine.imcode.expression;

import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * The constant NULL, i.e. an expression that always raises a SPARQL error.
 */
public final class SqlNull extends SqlExpressionIntercode
{
    /**
     * The only instance.
     */
    static private final SqlNull singleton = new SqlNull();


    /**
     * Creates the singleton.
     */
    private SqlNull()
    {
        super(Map.of(), true, true);
    }


    /**
     * The only instance.
     *
     * @return the only instance
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
    }


    @Override
    protected int getHashCode()
    {
        return SqlNull.class.getName().hashCode();
    }
}
