package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public abstract class SqlBinary extends SqlExpressionIntercode
{
    protected final SqlExpressionIntercode left;
    protected final SqlExpressionIntercode right;


    protected SqlBinary(SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;

        this.referencedVariables.addAll(left.getReferencedVariables());
        this.referencedVariables.addAll(right.getReferencedVariables());
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();
        restrictions.add(left.getRequirements());
        restrictions.add(right.getRequirements());

        return restrictions;
    }



    public SqlExpressionIntercode getLeft()
    {
        return left;
    }


    public SqlExpressionIntercode getRight()
    {
        return right;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinary imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!left.equals(imcode.left) || !right.equals(imcode.right))
            return false;

        return true;
    }
}
