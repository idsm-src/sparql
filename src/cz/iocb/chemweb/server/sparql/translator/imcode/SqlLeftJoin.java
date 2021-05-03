package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.expression.LeftJoinVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;



public class SqlLeftJoin extends SqlIntercode
{
    public static final String leftTable = "tab0";
    public static final String rightTable = "tab1";

    private final SqlIntercode left;
    private final SqlIntercode right;
    private final List<SqlExpressionIntercode> conditions;


    protected SqlLeftJoin(UsedVariables variables, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions)
    {
        super(variables, left.isDeterministic() && right.isDeterministic()
                && conditions.stream().allMatch(c -> c.isDeterministic()));
        this.left = left;
        this.right = right;
        this.conditions = conditions;
    }


    public static SqlIntercode leftJoin(DatabaseSchema schema, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> condition, HashSet<String> restrictions)
    {
        /* special cases */

        if(left == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(right == SqlNoSolution.get())
            return left;

        if(right == SqlEmptySolution.get())
            return left;

        if(left instanceof SqlUnion)
        {
            SqlUnion union = (SqlUnion) left;
            SqlIntercode newUnion = SqlNoSolution.get();

            for(SqlIntercode child : union.getChilds())
            {
                VariableAccessor accessor = new LeftJoinVariableAccessor(child.getVariables(), right.getVariables());

                List<SqlExpressionIntercode> optimizedCondition = condition.stream()
                        .map(f -> SqlEffectiveBooleanValue.create(f.optimize(accessor))).collect(Collectors.toList());

                newUnion = SqlUnion.union(newUnion, leftJoin(schema, child, right, optimizedCondition, restrictions));
            }

            return newUnion;
        }

        if(right instanceof SqlUnion)
            right = optimizeRightUnion(left.getVariables(), (SqlUnion) right);


        if(left instanceof SqlTableAccess && right instanceof SqlTableAccess && condition == null)
            if(SqlTableAccess.canBeLeftMerged(schema, (SqlTableAccess) left, (SqlTableAccess) right))
                return SqlTableAccess.leftMerge((SqlTableAccess) left, (SqlTableAccess) right);


        /* standard left join */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        UsedVariables variables = new UsedVariables();


        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            String var = pair.getName();
            boolean canBeLeftNull = leftVariable == null ? true : leftVariable.canBeNull();

            UsedVariable variable = new UsedVariable(var, canBeLeftNull);

            if(restrictions == null || restrictions.contains(var))
                variables.add(variable);

            int produceOnCondition = 0;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                {
                    if(leftVariable != null && !leftVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getRightClass());
                    produceOnCondition++;
                }
                else if(pairedClass.getRightClass() == null)
                {
                    variable.addClass(pairedClass.getLeftClass()); // left class is always included in the left join result

                    if(rightVariable != null && !rightVariable.canBeNull())
                        continue;

                    produceOnCondition++;
                }
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    variable.addClass(pairedClass.getLeftClass());
                    produceOnCondition++;
                }
                else if(pairedClass.getLeftClass().getGeneralClass() == pairedClass.getRightClass())
                {
                    if(leftVariable.canBeNull())
                        variable.addClass(pairedClass.getRightClass());
                    else
                        variable.addClass(pairedClass.getLeftClass());

                    produceOnCondition++;
                }
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass().getGeneralClass())
                {
                    variable.addClass(pairedClass.getLeftClass());
                    produceOnCondition++;
                }
                else
                {
                    assert false;
                }
            }

            // If no 'on condition' is produced, then the whole condition will be false
            // and therefore the left join can be eliminated./
            if(produceOnCondition == 0)
                return left;
        }


        return new SqlLeftJoin(variables, left, right, condition);
    }


    private static SqlIntercode optimizeRightUnion(UsedVariables variables, SqlUnion union)
    {
        SqlIntercode newUnion = SqlNoSolution.get();

        for(SqlIntercode child : union.getChilds())
            if(areVariablesCompatible(variables, child.getVariables()))
                newUnion = SqlUnion.union(newUnion, child);

        return newUnion;
    }


    private static boolean areVariablesCompatible(UsedVariables left, UsedVariables right)
    {
        for(UsedVariable leftVariable : left.getValues())
        {
            if(leftVariable.canBeNull())
                continue;

            UsedVariable rightVariable = right.get(leftVariable.getName());


            if(rightVariable == null || rightVariable.canBeNull())
                continue;

            boolean isCorrect = false;

            for(ResourceClass resClass : leftVariable.getClasses())
                if(rightVariable.getClasses().contains(resClass))
                    isCorrect = true;

            if(!isCorrect)
                return false;
        }

        return true;
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        reduced = reduced & conditions.stream().allMatch(r -> r.isDeterministic());

        HashSet<String> childRestrictions = new HashSet<String>();
        childRestrictions.addAll(left.getVariables().getNames());
        childRestrictions.retainAll(right.getVariables().getNames());
        childRestrictions.addAll(restrictions);

        for(SqlExpressionIntercode condition : conditions)
            childRestrictions.addAll(condition.getVariables());

        return leftJoin(request.getConfiguration().getDatabaseSchema(),
                left.optimize(request, childRestrictions, reduced), right.optimize(request, childRestrictions, reduced),
                conditions, restrictions);
    }


    @Override
    public String translate()
    {
        /* join */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        Set<ResourceClass> emptySet = new HashSet<ResourceClass>();

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");

        boolean hasSelect = false;

        for(UsedVariable variable : variables.getValues())
        {
            String var = variable.getName();
            UsedVariable leftVar = left.getVariables().get(var);
            UsedVariable rightVar = right.getVariables().get(var);


            for(ResourceClass resClass : variable.getClasses())
            {
                boolean leftCanBeNull = leftVar == null ? true : leftVar.canBeNull();
                boolean rightCanBeNull = rightVar == null ? true : rightVar.canBeNull();
                Set<ResourceClass> leftClasses = leftVar != null ? leftVar.getCompatible(resClass) : emptySet;
                Set<ResourceClass> rightClasses = rightVar != null ? rightVar.getCompatible(resClass) : emptySet;


                if(!leftCanBeNull && rightCanBeNull)
                    rightClasses = emptySet;
                else if(!leftCanBeNull && !rightCanBeNull && !leftClasses.isEmpty())
                    rightClasses = emptySet;


                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    generateJoinSelectVariable(builder, var, resClass, i, leftClasses, rightClasses);
                }
            }
        }


        builder.append(" FROM ");
        builder.append("(");
        builder.append(left.translate());
        builder.append(") AS ");
        builder.append(leftTable);

        builder.append(" LEFT OUTER JOIN (");
        builder.append(right.translate());
        builder.append(") AS ");
        builder.append(rightTable);


        builder.append(" ON ");

        boolean hasWhere = false;

        for(UsedPairedVariable pair : pairs)
        {
            String var = pair.getName();
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                appendAnd(builder, hasWhere);
                hasWhere = true;

                builder.append("(");
                boolean restricted = false;

                if(leftVariable.canBeNull())
                {
                    boolean use = false;
                    builder.append("(");

                    for(ResourceClass resClass : leftVariable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, use);
                            use = true;

                            builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                            builder.append(" IS NULL");
                        }
                    }

                    builder.append(")");

                    assert use;
                    restricted = true;
                }

                if(rightVariable.canBeNull())
                {
                    appendOr(builder, restricted);
                    restricted = true;

                    boolean use = false;
                    builder.append("(");

                    for(ResourceClass resClass : rightVariable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, use);
                            use = true;

                            builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
                            builder.append(" IS NULL");
                        }
                    }

                    builder.append(")");

                    assert use;
                }

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                    {
                        appendOr(builder, restricted);
                        restricted = true;

                        builder.append("(");

                        if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                        {
                            ResourceClass resClass = pairedClass.getLeftClass();

                            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);

                                builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                                builder.append(" = ");
                                builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
                            }
                        }
                        else
                        {
                            ResourceClass resClass = pairedClass.getLeftClass().getGeneralClass();

                            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);

                                builder.append(
                                        pairedClass.getLeftClass().getGeneralisedPatternCode(leftTable, var, i, false));
                                builder.append(" = ");
                                builder.append(pairedClass.getRightClass().getGeneralisedPatternCode(rightTable, var, i,
                                        false));
                            }
                        }

                        builder.append(")");
                    }
                }

                builder.append(")");

                assert restricted;
            }
        }

        for(SqlExpressionIntercode condition : conditions)
        {
            appendAnd(builder, hasWhere);
            hasWhere = true;

            builder.append(condition.translate());
        }

        if(!hasWhere)
            builder.append("true");


        return builder.toString();
    }


    private void generateJoinSelectVariable(StringBuilder builder, String var, ResourceClass resourceClass, int part,
            Set<ResourceClass> leftClasses, Set<ResourceClass> rightClasses)
    {
        if(leftClasses.size() + rightClasses.size() > 1)
            builder.append("COALESCE(");

        boolean hasVariant = false;

        for(ResourceClass leftClass : leftClasses)
        {
            appendComma(builder, hasVariant);
            hasVariant = true;

            if(leftClass == resourceClass)
                builder.append(leftTable).append('.').append(leftClass.getSqlColumn(var, part));
            else
                builder.append(leftClass.getGeneralisedPatternCode(leftTable, var, part, true));
        }

        for(ResourceClass rightClass : rightClasses)
        {
            appendComma(builder, hasVariant);
            hasVariant = true;

            if(rightClass == resourceClass)
                builder.append(rightTable).append('.').append(rightClass.getSqlColumn(var, part));
            else
                builder.append(rightClass.getGeneralisedPatternCode(rightTable, var, part, true));
        }

        if(leftClasses.size() + rightClasses.size() > 1)
            builder.append(") AS ").append(resourceClass.getSqlColumn(var, part));
    }
}
