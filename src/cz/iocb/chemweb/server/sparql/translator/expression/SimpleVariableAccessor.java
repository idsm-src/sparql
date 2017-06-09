package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
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
    public UsedVariable getUsedVariable(Variable variable)
    {
        return usedVariables.get(variable.getName());
    }


    @Override
    public String variableAccess(Variable variable, ResourceClass resourceClass, int i)
    {
        return resourceClass.getSqlColumn(variable.getName(), i);
    }
}
