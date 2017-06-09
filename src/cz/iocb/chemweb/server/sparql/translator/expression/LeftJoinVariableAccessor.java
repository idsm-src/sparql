package cz.iocb.chemweb.server.sparql.translator.expression;

import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlLeftJoin;



public class LeftJoinVariableAccessor extends SimpleVariableAccessor
{
    private static final String leftTable = SqlLeftJoin.leftTable;
    private static final String rightTable = SqlLeftJoin.rightTable;

    private final UsedVariables left;
    private final UsedVariables right;


    public LeftJoinVariableAccessor(UsedVariables left, UsedVariables right)
    {
        super(generateUsedVariables(left, right));
        this.left = left;
        this.right = right;
    }


    private static UsedVariables generateUsedVariables(UsedVariables left, UsedVariables right)
    {
        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left, right);
        UsedVariables variables = new UsedVariables();


        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();


            String var = pair.getName();
            boolean canBeLeftNull = leftVariable == null ? true : leftVariable.canBeNull();
            boolean canBeRightNull = rightVariable == null ? true : rightVariable.canBeNull();

            UsedVariable variable = new UsedVariable(var, canBeLeftNull && canBeRightNull);
            variables.add(variable);


            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                {
                    if(leftVariable != null && !leftVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getRightClass());
                }
                else if(pairedClass.getRightClass() == null)
                {
                    if(rightVariable != null && !rightVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getLeftClass());
                }
                else
                {
                    assert pairedClass.getLeftClass() == pairedClass.getRightClass();
                    variable.addClass(pairedClass.getLeftClass());
                }
            }
        }


        return variables;
    }


    @Override
    public String variableAccess(Variable variable, ResourceClass resourceClass, int i)
    {
        UsedVariable leftVariable = left.get(variable.getName());
        UsedVariable rightVariable = right.get(variable.getName());


        if(rightVariable == null || rightVariable != null && !rightVariable.containsClass(resourceClass)
                || leftVariable != null && !leftVariable.canBeNull())
        {
            return leftTable + "." + resourceClass.getSqlColumn(variable.getName(), i);
        }
        else if(leftVariable == null || leftVariable != null && !leftVariable.containsClass(resourceClass)
                || rightVariable != null && !rightVariable.canBeNull())
        {
            return rightTable + "." + resourceClass.getSqlColumn(variable.getName(), i);
        }
        else
        {
            return "COALESCE(" + leftTable + "." + resourceClass.getSqlColumn(variable.getName(), i) + ", " + rightTable
                    + "." + resourceClass.getSqlColumn(variable.getName(), i) + ")";
        }
    }
}
