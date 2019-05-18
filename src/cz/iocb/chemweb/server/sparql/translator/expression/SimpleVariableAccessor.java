package cz.iocb.chemweb.server.sparql.translator.expression;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SimpleVariableAccessor extends VariableAccessor
{
    private final UsedVariables usedVariables;
    private final String table;


    public SimpleVariableAccessor(UsedVariables usedVariables, String table)
    {
        this.usedVariables = usedVariables;
        this.table = table;
    }


    public SimpleVariableAccessor(UsedVariables usedVariables)
    {
        this(usedVariables, "tab");
    }


    @Override
    public UsedVariables getUsedVariables()
    {
        return usedVariables;
    }


    @Override
    public UsedVariable getUsedVariable(String variable)
    {
        return usedVariables.get(variable);
    }


    @Override
    public String getSqlVariableAccess(String variable, ResourceClass resourceClass, int part)
    {
        Set<ResourceClass> resClasses = usedVariables.get(variable).getCompatible(resourceClass);

        StringBuilder builder = new StringBuilder();

        if(resClasses.size() > 1)
            builder.append("COALESCE(");

        boolean hasVariant = false;

        for(ResourceClass resClass : resClasses)
        {
            if(hasVariant)
                builder.append(", ");
            else
                hasVariant = true;

            if(resClass == resourceClass)
                builder.append(table).append('.').append(resClass.getSqlColumn(variable, part));
            else
                builder.append(resClass.getGeneralisedPatternCode(table, variable, part, true));
        }

        if(resClasses.size() > 1)
            builder.append(")");

        return builder.toString();
    }
}
