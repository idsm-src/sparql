package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.google.common.collect.Lists;
import cz.iocb.chemweb.server.db.schema.DatabaseSchema;
import cz.iocb.chemweb.server.db.schema.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final ArrayList<SqlIntercode> childs;


    protected SqlJoin(UsedVariables variables, ArrayList<SqlIntercode> childs)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));
        this.childs = childs;
    }


    public static SqlIntercode join(DatabaseSchema schema, SqlIntercode left, SqlIntercode right)
    {
        return join(schema, left, right, null);
    }


    public static SqlIntercode join(DatabaseSchema schema, SqlIntercode left, SqlIntercode right,
            HashSet<String> restrictions)
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
                    union = SqlUnion.union(union, join(schema, leftChild, rightChild, restrictions));

            return union;
        }


        /* standard join */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        UsedVariables variables = getUsedVariable(pairs, restrictions);

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

        SqlJoin join = new SqlJoin(variables, childs);


        return optimize(join, schema, restrictions);
    }


    private static SqlIntercode optimize(SqlJoin join, DatabaseSchema schema, HashSet<String> restrictions)
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
                        HashSet<String> subrestrictions = getRestrictions(join, i, j, restrictions);
                        SqlTableAccess merge = SqlTableAccess.merge(left, right, variantA, subrestrictions);

                        join.childs.set(j, merge);
                        join.childs.remove(i);
                        continue loop;
                    }


                    List<ColumnPair> variantB = SqlTableAccess.canBeDroped(schema, right, left);

                    if(variantB != null)
                    {
                        HashSet<String> subrestrictions = getRestrictions(join, i, j, restrictions);
                        SqlTableAccess merge = SqlTableAccess.merge(right, left, variantB, subrestrictions);

                        join.childs.set(i, merge);
                        join.childs.remove(j);
                        continue loop;
                    }


                    if(SqlTableAccess.canBeMerged(schema, left, right))
                    {
                        HashSet<String> subrestrictions = getRestrictions(join, i, j, restrictions);
                        SqlTableAccess merge = SqlTableAccess.merge(left, right, subrestrictions);

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
    public SqlIntercode optimize(DatabaseSchema schema, HashSet<String> restrictions, boolean reduced)
    {
        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        HashMap<String, Integer> variableCounts = new HashMap<String, Integer>();

        for(SqlIntercode child : childs)
            for(String var : child.getVariables().getNames())
                variableCounts.put(var, variableCounts.get(var) == null ? 1 : variableCounts.get(var) + 1);

        for(Entry<String, Integer> entry : variableCounts.entrySet())
            if(entry.getValue() > 1)
                childRestrictions.add(entry.getKey());


        SqlIntercode result = new SqlEmptySolution();

        for(SqlIntercode child : childs)
            result = join(schema, result, child.optimize(schema, childRestrictions, reduced), restrictions);

        return result;
    }


    @Override
    public String translate()
    {
        @SuppressWarnings("unchecked")
        ArrayList<SqlIntercode> childs = (ArrayList<SqlIntercode>) this.childs.clone();
        childs.sort(new Comparator<SqlIntercode>()
        {
            @Override
            public int compare(SqlIntercode i1, SqlIntercode i2)
            {
                if(i1 instanceof SqlValues && !(i2 instanceof SqlValues))
                    return 1;
                if(i2 instanceof SqlValues && !(i1 instanceof SqlValues))
                    return -1;
                else
                    return 0;
            }
        });


        String joinResult = childs.get(0).translate();
        UsedVariables joinVariables = childs.get(0).getVariables();

        for(int idx = 1; idx < childs.size(); idx++)
        {
            SqlIntercode child = childs.get(idx);
            ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(joinVariables, child.getVariables());

            HashSet<String> restrictions = new HashSet<String>(variables.getNames());

            for(int i = idx + 1; i < childs.size(); i++)
                restrictions.addAll(childs.get(i).getVariables().getNames());

            UsedVariables variables = getUsedVariable(pairs, restrictions);


            SimpleValuesJoin:
            if(child instanceof SqlValues)
            {
                HashSet<String> shared = new HashSet<String>();

                for(UsedVariable var : child.getVariables().getValues())
                {
                    UsedVariable joinVar = joinVariables.get(var.getName());

                    if(joinVar != null)
                    {
                        shared.add(var.getName());

                        if(var.canBeNull() || joinVar.canBeNull())
                            break SimpleValuesJoin;
                    }
                    else
                    {
                        if(restrictions.contains(var.getName()))
                            break SimpleValuesJoin;
                    }
                }

                SqlValues values = (SqlValues) child;

                for(int k = 0; k < values.typedValuesList.size(); k++)
                {
                    for(int l = k + 1; l < values.typedValuesList.size(); l++)
                    {
                        boolean equal = true;

                        for(int i = 0; i < values.typedValuesList.get(k).size(); i++)
                        {
                            if(shared.contains(values.typedVariables.get(i).getKey()))
                            {
                                Node n1 = values.typedValuesList.get(k).get(i).getKey();
                                Node n2 = values.typedValuesList.get(l).get(i).getKey();

                                if(!n1.equals(n2))
                                    equal = false;
                            }
                        }

                        if(equal)
                            break SimpleValuesJoin;
                    }
                }

                for(UsedPairedVariable pair : pairs)
                {
                    if(shared.contains(pair.getName()))
                    {
                        for(PairedClass pairedClass : pair.getClasses())
                        {
                            if(pairedClass.getRightClass() != null
                                    && pairedClass.getLeftClass() != pairedClass.getRightClass())
                                break SimpleValuesJoin;
                        }
                    }
                }


                StringBuilder builder = new StringBuilder();

                builder.append("SELECT ");
                boolean hasSelect = false;

                for(UsedVariable variable : variables.getValues())
                {
                    for(ResourceClass resClass : variable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendComma(builder, hasSelect);
                            hasSelect = true;

                            builder.append(resClass.getSqlColumn(variable.getName(), i));
                        }
                    }
                }

                if(!hasSelect)
                    builder.append("1");

                builder.append(" FROM (");
                builder.append(joinResult);
                builder.append(" ) AS tab");

                if(!shared.isEmpty())
                {
                    builder.append(" WHERE ");

                    boolean hasWhere = false;

                    conditions:
                    for(ArrayList<Pair<Node, ResourceClass>> row : values.typedValuesList)
                    {
                        for(int i = 0; i < values.typedVariables.size(); i++)
                        {
                            String varName = values.typedVariables.get(i).getKey();

                            if(shared.contains(varName)
                                    && !child.getVariables().contains(varName, row.get(i).getValue()))
                                continue conditions;
                        }


                        appendOr(builder, hasWhere);
                        hasWhere = true;

                        builder.append("(");

                        boolean hasPart = false;

                        for(int i = 0; i < values.typedVariables.size(); i++)
                        {
                            String varName = values.typedVariables.get(i).getKey();

                            if(shared.contains(varName))
                            {
                                Node node = row.get(i).getKey();
                                ResourceClass resClass = row.get(i).getValue();

                                for(int j = 0; j < resClass.getPatternPartsCount(); j++)
                                {
                                    appendAnd(builder, hasPart);
                                    hasPart = true;

                                    builder.append(resClass.getSqlColumn(varName, j));
                                    builder.append(" = ");
                                    builder.append(resClass.getPatternCode(node, j));
                                }

                            }
                        }

                        builder.append(")");
                    }

                    if(!hasWhere)
                        builder.append("true");
                }

                joinResult = builder.toString();
                joinVariables = variables;

                continue;
            }



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


    private static HashSet<String> getRestrictions(SqlJoin join, int i, int j, HashSet<String> restrictions)
    {
        if(restrictions == null)
            return null;

        HashSet<String> subrestrictions = new HashSet<String>(restrictions);

        for(int k = 0; k < join.childs.size(); k++)
            if(k != i && k != j)
                subrestrictions.addAll(join.childs.get(k).getVariables().getNames());

        return subrestrictions;
    }


    private static UsedVariables getUsedVariable(ArrayList<UsedPairedVariable> pairs, HashSet<String> restrictions)
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

            if(restrictions == null || restrictions.contains(var))
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
