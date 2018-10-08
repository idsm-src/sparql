package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SimpleVariableAccessor extends VariableAccessor
{
    private final UsedVariables usedVariables;


    public SimpleVariableAccessor(UsedVariables usedVariables)
    {
        this.usedVariables = usedVariables;
    }


    @Override
    public UsedVariable getUsedVariable(String variable)
    {
        return usedVariables.get(variable);
    }


    @Override
    public String getSqlVariableAccess(String variable, ResourceClass resourceClass, int part)
    {
        return resourceClass.getSqlColumn(variable, part);
    }
}
