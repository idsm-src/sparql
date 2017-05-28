package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.Lists;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final ArrayList<SqlIntercode> childs;


    protected SqlJoin(UsedVariables variables, ArrayList<SqlIntercode> childs)
    {
        super(variables);
        this.childs = childs;
    }


    public static SqlIntercode join(DatabaseSchema schema, SqlIntercode left, SqlIntercode right)
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


            SqlIntercode union = null;

            for(SqlIntercode leftChild : leftChilds)
            {
                for(SqlIntercode rightChild : rightChilds)
                {
                    SqlIntercode join = join(schema, leftChild, rightChild);

                    if(union == null)
                        union = join;
                    else
                        union = SqlUnion.union(union, join);
                }
            }

            return union;
        }


        /* standard join */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
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

            if(variable.getClasses().isEmpty())
                return new SqlNoSolution();
        }


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


        return optimize(join, schema);
    }


    private static SqlIntercode optimize(SqlJoin join, DatabaseSchema schema)
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


                    List<KeyPair> variantA = SqlTableAccess.canBeDroped(schema, left, right);

                    if(variantA != null)
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(left, right, variantA);
                        join.childs.set(j, merge);
                        join.childs.remove(i);
                        continue loop;
                    }


                    List<KeyPair> variantB = SqlTableAccess.canBeDroped(schema, right, left);

                    if(variantB != null)
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(right, left, variantB);
                        join.childs.set(i, merge);
                        join.childs.remove(j);
                        continue loop;
                    }


                    if(SqlTableAccess.canBeMerged(schema, left, right))
                    {
                        SqlTableAccess merge = SqlTableAccess.merge(left, right);

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
            UsedVariables variables = new UsedVariables();
            StringBuilder builder = new StringBuilder();


            builder.append("SELECT ");

            boolean hasSelect = false;

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


                    ResourceClass resClass = pairedClass.getLeftClass() != null ? pairedClass.getLeftClass()
                            : pairedClass.getRightClass();

                    for(int i = 0; i < resClass.getPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        generateJoinSelectVarable(builder, leftVariable, rightVariable, resClass.getSqlColumn(var, i));
                    }
                }
            }


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
                            for(int i = 0; i < resClass.getPartsCount(); i++)
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
                            for(int i = 0; i < resClass.getPartsCount(); i++)
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
                            assert pairedClass.getLeftClass() == pairedClass.getRightClass();
                            ResourceClass resClass = pairedClass.getLeftClass();

                            appendOr(builder, restricted);
                            restricted = true;

                            builder.append("(");

                            for(int i = 0; i < resClass.getPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);

                                builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                                builder.append(" = ");
                                builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
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


    private void generateJoinSelectVarable(StringBuilder builder, UsedVariable leftVariable, UsedVariable rightVariable,
            String var)
    {
        if(leftVariable == null)
        {
            builder.append(rightTable).append('.').append(var);
        }
        else if(rightVariable == null)
        {
            builder.append(leftTable).append('.').append(var);
        }
        else if(!leftVariable.canBeNull())
        {
            builder.append(leftTable).append('.').append(var);
        }
        else if(!rightVariable.canBeNull())
        {
            builder.append(rightTable).append('.').append(var);
        }
        else
        {
            builder.append("COALESCE(");
            builder.append(leftTable).append('.').append(var);
            builder.append(", ");
            builder.append(rightTable).append('.').append(var);
            builder.append(")");
        }

        builder.append(" AS ");
        builder.append(var);
    }
}
