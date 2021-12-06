package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlExpressionError extends SqlExpressionIntercode
{
    public static SqlExpressionError create()
    {
        return new SqlExpressionError(new HashSet<ResourceClass>(), true);
    }


    protected SqlExpressionError(Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull, true);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return null;
    }
}
