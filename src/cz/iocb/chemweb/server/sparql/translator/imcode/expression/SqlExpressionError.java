package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlExpressionError extends SqlExpressionIntercode
{
    public static SqlExpressionError create()
    {
        return new SqlExpressionError(BuiltinClasses.getClasses().stream().collect(Collectors.toSet()), true);
    }


    protected SqlExpressionError(Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull, true);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return null;
    }
}
