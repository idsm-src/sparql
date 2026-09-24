package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Expression with one operand.
 */
public abstract class SqlUnary extends SqlExpressionIntercode
{
    /**
     * The operand.
     */
    protected final SqlExpressionIntercode operand;


    /**
     * Creates the expression; deterministic if the operand is.
     *
     * @param operand the operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    protected SqlUnary(SqlExpressionIntercode operand, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, operand.isDeterministic());
        this.operand = operand;

        this.referencedVariables.addAll(operand.getReferencedVariables());
    }


    @Override
    public Restrictions getRequirements()
    {
        return operand.getRequirements();
    }


    /**
     * The operand.
     *
     * @return the operand
     */
    public SqlExpressionIntercode getOperand()
    {
        return operand;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlUnary imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!operand.equals(imcode.operand))
            return false;

        return true;
    }
}
