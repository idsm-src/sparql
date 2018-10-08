package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public abstract class SqlBinary extends SqlExpressionIntercode
{
    private final SqlExpressionIntercode left;
    private final SqlExpressionIntercode right;


    protected SqlBinary(SqlExpressionIntercode left, SqlExpressionIntercode right, Set<ResourceClass> resourceClasses,
            boolean isBoxed, boolean canBeNull)
    {
        super(resourceClasses, isBoxed, canBeNull);
        this.left = left;
        this.right = right;
    }


    public SqlExpressionIntercode getLeft()
    {
        return left;
    }


    public SqlExpressionIntercode getRight()
    {
        return right;
    }
}
