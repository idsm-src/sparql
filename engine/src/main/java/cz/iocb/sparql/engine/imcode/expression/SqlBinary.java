package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Expression with two operands.
 */
public abstract class SqlBinary extends SqlExpressionIntercode
{
    /**
     * Left operand.
     */
    protected final SqlExpressionIntercode left;

    /**
     * Right operand.
     */
    protected final SqlExpressionIntercode right;


    /**
     * Creates the expression; deterministic if both operands are.
     *
     * @param left the left operand
     * @param right the right operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
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



    /**
     * Left operand.
     *
     * @return left operand
     */
    public SqlExpressionIntercode getLeft()
    {
        return left;
    }


    /**
     * Right operand.
     *
     * @return right operand
     */
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
