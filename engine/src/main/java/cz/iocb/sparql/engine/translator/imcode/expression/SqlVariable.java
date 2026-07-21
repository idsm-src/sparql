package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlVariable extends SqlExpressionIntercode
{
    private final String name;


    private SqlVariable(String name, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.name = name;
        this.referencedVariables.add(name);
    }


    public static SqlExpressionIntercode create(UsedVariable variable)
    {
        return create(variable, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(UsedVariable variable, Restriction restriction)
    {
        if(variable == null)
            return SqlNull.get();

        return new SqlVariable(variable.getName(), restriction.restrict(variable.getMappings()), variable.canBeNull());
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();

        for(Entry<ResourceClass, List<Column>> e : variable.getMappings().entrySet())
            if(e.getValue() != null)
                restrictions.add(name, e.getKey());

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
            boolean evalServices)
    {
        UsedVariable var = variables.get(name);

        if(var == null)
            return SqlNull.get();


        if(variable.equals(var) && restriction.isOptimized(variable))
            return this;

        return create(var, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(name);
    }


    public String getName()
    {
        return name;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlVariable imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!name.equals(imcode.name))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(name);
    }
}
