package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public abstract class VariableAccessor
{
    public abstract UsedVariable getUsedVariable(Variable variable);

    public abstract String variableAccess(Variable variable, ResourceClass resourceClass, int i);
}
