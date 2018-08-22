package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public abstract class VariableAccessor
{
    public abstract UsedVariable getUsedVariable(String variable);

    public abstract String variableAccess(String variable, PatternResourceClass resourceClass, int i);
}
