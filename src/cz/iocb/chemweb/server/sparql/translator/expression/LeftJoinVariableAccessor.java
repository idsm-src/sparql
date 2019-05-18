package cz.iocb.chemweb.server.sparql.translator.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlLeftJoin;



public class LeftJoinVariableAccessor extends VariableAccessor
{
    private static final String leftTable = SqlLeftJoin.leftTable;
    private static final String rightTable = SqlLeftJoin.rightTable;

    private final UsedVariables left;
    private final UsedVariables right;
    private final UsedVariables usedVariables;


    public LeftJoinVariableAccessor(UsedVariables left, UsedVariables right)
    {
        this.left = left;
        this.right = right;
        this.usedVariables = generateUsedVariables(left, right);
    }


    @Override
    public UsedVariables getUsedVariables()
    {
        return usedVariables;
    }


    @Override
    public UsedVariable getUsedVariable(String variable)
    {
        return usedVariables.get(variable);
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
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    variable.addClass(pairedClass.getLeftClass());
                }
                else if(pairedClass.getLeftClass().getGeneralClass() == pairedClass.getRightClass())
                {
                    if(leftVariable.canBeNull())
                        variable.addClass(pairedClass.getRightClass());
                    else
                        variable.addClass(pairedClass.getLeftClass());
                }
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass().getGeneralClass())
                {
                    variable.addClass(pairedClass.getLeftClass());
                }
            }
        }


        return variables;
    }


    @Override
    public String getSqlVariableAccess(String variable, ResourceClass resourceClass, int part)
    {
        Set<ResourceClass> emptySet = new HashSet<ResourceClass>();

        UsedVariable leftVar = left.get(variable);
        UsedVariable rightVar = right.get(variable);

        boolean leftCanBeNull = leftVar == null ? true : leftVar.canBeNull();
        boolean rightCanBeNull = rightVar == null ? true : rightVar.canBeNull();
        Set<ResourceClass> leftClasses = leftVar != null ? leftVar.getCompatible(resourceClass) : emptySet;
        Set<ResourceClass> rightClasses = rightVar != null ? rightVar.getCompatible(resourceClass) : emptySet;


        if(!leftCanBeNull && rightCanBeNull)
            rightClasses = emptySet;
        else if(!leftCanBeNull && !rightCanBeNull && !leftClasses.isEmpty())
            rightClasses = emptySet;


        StringBuilder builder = new StringBuilder();

        if(leftClasses.size() + rightClasses.size() > 1)
            builder.append("COALESCE(");

        boolean hasVariant = false;

        for(ResourceClass leftClass : leftClasses)
        {
            if(hasVariant)
                builder.append(", ");
            else
                hasVariant = true;

            if(leftClass == resourceClass)
                builder.append(leftTable).append('.').append(leftClass.getSqlColumn(variable, part));
            else
                builder.append(leftClass.getGeneralisedPatternCode(leftTable, variable, part, true));
        }

        for(ResourceClass rightClass : rightClasses)
        {
            if(hasVariant)
                builder.append(", ");
            else
                hasVariant = true;

            if(rightClass == resourceClass)
                builder.append(rightTable).append('.').append(rightClass.getSqlColumn(variable, part));
            else
                builder.append(rightClass.getGeneralisedPatternCode(rightTable, variable, part, true));
        }

        if(leftClasses.size() + rightClasses.size() > 1)
            builder.append(")");


        return builder.toString();
    }
}
