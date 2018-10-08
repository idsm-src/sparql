package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public abstract class VariableAccessor
{
    public abstract UsedVariable getUsedVariable(String variable);

    public abstract String getSqlVariableAccess(String variable, ResourceClass resourceClass, int part);
}
