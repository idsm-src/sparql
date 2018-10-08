package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public abstract class SqlNodeValue extends SqlExpressionIntercode
{
    protected SqlNodeValue(Set<ResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
    {
        super(resourceClasses, isBoxed, canBeNull);
    }


    public abstract String getNodeAccess(ResourceClass resourceClass, int part);
}
