package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.Lists;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final ArrayList<SqlIntercode> childs;
    private final boolean strip;


    protected SqlJoin(UsedVariables variables, ArrayList<SqlIntercode> childs, boolean strip)
    {
        super(variables);
        this.childs = childs;
        this.strip = strip;
    }


    public static SqlIntercode join(DatabaseSchema schema, SqlIntercode left, SqlIntercode right)
    {
        return join(schema, left, right, false);
    }


    public static SqlIntercode join(DatabaseSchema schema, SqlIntercode left, SqlIntercode right, boolean strip)
    {
        /* special cases */

        if(left instanceof SqlNoSolution || right instanceof SqlNoSolution)
            return new SqlNoSolution();


        if(left instanceof SqlEmptySolution)
            return right;

        if(right instanceof SqlEmptySolution)
            return left;


        if(left instanceof SqlUnion || right instanceof SqlUnion)
        {
            ArrayList<SqlIntercode> leftChilds;

            if(left instanceof SqlUnion)
                leftChilds = ((SqlUnion) left).getChilds();
            else
                leftChilds = Lists.newArrayList(left);


            ArrayList<SqlIntercode> rightChilds;

            if(right instanceof SqlUnion)
                rightChilds = ((SqlUnion) right).getChilds();
            else
                rightChilds = Lists.newArrayList(right);


            SqlIntercode union = new SqlNoSolution();

            for(SqlIntercode leftChild : leftChilds)
                for(SqlIntercode rightChild : rightChilds)
                    union = SqlUnion.union(union, join(schema, leftChild, rightChild, strip));

            return union;
        }


        /* standard join */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        UsedVariables variables = getUsedVariable(pairs, strip);

        if(variables == null)
            return new SqlNoSolution();


        ArrayList<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        if(left instanceof SqlJoin)
            childs.addAll(((SqlJoin) left).childs);
        else
            childs.add(left);

        if(right instanceof SqlJoin)
            childs.addAll(((SqlJoin) right).childs);
        else
            childs.add(right);

        SqlJoin join = new SqlJoin(variables, childs, strip);


        return optimize(join, schema, strip);
    }


    private static SqlIntercode optimize(SqlJoin join, DatabaseSchema schema, boolean strip)
    {
        if(schema == null)
            return join;


        loop:
        while(true)
        {
            for(int i = 0; i < join.childs.size(); i++)
            {
                for(int j = i + 1; j < join.childs.size(); j++)
                {
                    SqlIntercode iSql = join.childs.get(i);
                    SqlIntercode jSql = join.childs.get(j);

                    if(!(iSql instanceof SqlTableAccess))
                        continue;

                    if(!(jSql instanceof SqlTableAccess))
                        continue;

                    SqlTableAccess left = (SqlTableAccess) iSql;
                    SqlTableAccess right = (SqlTableAccess) jSql;


                    if(!SqlTableAccess.areCompatible(schema, left, right))
                        return new SqlNoSolution();


                    List<ColumnPair> variantA = SqlTableAccess.canBeDroped(schema, left, right);

                    if(variantA != null)
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(left, right, variantA, strip);
                        join.childs.set(j, merge);
                        join.childs.remove(i);
                        continue loop;
                    }


                    List<ColumnPair> variantB = SqlTableAccess.canBeDroped(schema, right, left);

                    if(variantB != null)
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(right, left, variantB, strip);
                        join.childs.set(i, merge);
                        join.childs.remove(j);
                        continue loop;
                    }


                    if(SqlTableAccess.canBeMerged(schema, left, right))
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(left, right, strip);

                        join.childs.set(i, merge);
                        join.childs.remove(j);
                        continue loop;
                    }
                }
            }

            break loop;
        }


        if(join.childs.size() == 1)
            return join.childs.get(0);

        return join;
    }


    @Override
    public String translate()
    {
        String joinResult = null;
        UsedVariables joinVariables = null;


        for(SqlIntercode child : childs)
        {
            if(joinResult == null)
            {
                joinResult = child.translate();
                joinVariables = child.getVariables();
                continue;
            }


            /* join */

            ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(joinVariables, child.getVariables());
            UsedVariables variables = getUsedVariable(pairs, strip);
            Set<ResourceClass> emptySet = new HashSet<ResourceClass>();

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT ");

            boolean hasSelect = false;

            for(UsedVariable variable : variables.getValues())
            {
                String var = variable.getName();
                UsedVariable leftVar = joinVariables.get(var);
                UsedVariable rightVar = child.getVariables().get(var);


                for(ResourceClass resClass : variable.getClasses())
                {
                    boolean leftCanBeNull = leftVar == null ? true : leftVar.canBeNull();
                    boolean rightCanBeNull = rightVar == null ? true : rightVar.canBeNull();
                    Set<ResourceClass> leftClasses = leftVar != null ? leftVar.getCompatible(resClass) : emptySet;
                    Set<ResourceClass> rightClasses = rightVar != null ? rightVar.getCompatible(resClass) : emptySet;


                    if(leftCanBeNull && !rightCanBeNull)
                        leftClasses = emptySet;
                    else if(!leftCanBeNull && rightCanBeNull)
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

            if(!hasSelect)
                builder.append(" 1 ");


            builder.append(" FROM ");
            builder.append("(");
            builder.append(joinResult);
            builder.append(") AS ");
            builder.append(leftTable);

            builder.append(", (");
            builder.append(child.translate());
            builder.append(") AS ");
            builder.append(rightTable);


            builder.append(" WHERE ");

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

                                    builder.append(pairedClass.getLeftClass().getGeneralisedPatternCode(leftTable, var,
                                            i, false));
                                    builder.append(" = ");
                                    builder.append(pairedClass.getRightClass().getGeneralisedPatternCode(rightTable,
                                            var, i, false));
                                }
                            }

                            builder.append(")");
                        }
                    }

                    builder.append(")");

                    assert restricted;
                }
            }

            if(!hasWhere)
                builder.append("true");



            joinResult = builder.toString();
            joinVariables = variables;
        }


        return joinResult;
    }


    private static UsedVariables getUsedVariable(ArrayList<UsedPairedVariable> pairs, boolean strip)
    {
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();


            String var = pair.getName();
            boolean canBeLeftNull = leftVariable == null ? true : leftVariable.canBeNull();
            boolean canBeRightNull = rightVariable == null ? true : rightVariable.canBeNull();

            UsedVariable variable = new UsedVariable(var, canBeLeftNull && canBeRightNull);

            if(strip == false || leftVariable == null || rightVariable == null)
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
                    if(rightVariable.canBeNull())
                        variable.addClass(pairedClass.getLeftClass());
                    else
                        variable.addClass(pairedClass.getRightClass());
                }
                else
                {
                    assert false;
                }
            }

            if(variable.getClasses().isEmpty())
                return null;
        }

        return variables;
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
