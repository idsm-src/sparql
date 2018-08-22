package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public abstract class SqlNodeValue extends SqlExpressionIntercode
{
    protected SqlNodeValue(Set<ExpressionResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
    {
        super(resourceClasses, isBoxed, canBeNull);
    }


    public abstract Set<PatternResourceClass> getPatternResourceClasses();


    public abstract String nodeAccess(PatternResourceClass resourceClass, int part);
}
