package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public abstract class SqlUnary extends SqlExpressionIntercode
{
    private final SqlExpressionIntercode operand;


    protected SqlUnary(SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull);
        this.operand = operand;
    }


    public SqlExpressionIntercode getOperand()
    {
        return operand;
    }
}
