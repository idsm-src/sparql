package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlMinus extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final SqlIntercode left;
    private final SqlIntercode right;


    protected SqlMinus(UsedVariables variables, SqlIntercode left, SqlIntercode right)
    {
        super(variables, left.isDeterministic() && right.isDeterministic());
        this.left = left;
        this.right = right;
    }


    public static SqlIntercode minus(SqlIntercode left, SqlIntercode right, HashSet<String> restrictions)
    {
        boolean shareVariables = false;

        for(UsedVariable variable : right.getVariables().getValues())
            if(left.getVariables().get(variable.getName()) != null)
                shareVariables = true;

        if(shareVariables == false)
            return left;

        return new SqlMinus(left.getVariables().restrict(restrictions), left, right);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        HashSet<String> childRestrictions = new HashSet<String>();
        childRestrictions.addAll(left.getVariables().getNames());
        childRestrictions.retainAll(right.getVariables().getNames());
        childRestrictions.addAll(restrictions);

        return minus(left.optimize(request, childRestrictions, reduced),
                right.optimize(request, childRestrictions, reduced), restrictions);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(UsedVariable variable : variables.getValues())
        {
            String varName = variable.getName();

            for(ResourceClass resClass : variable.getClasses())
            {
                for(int j = 0; j < resClass.getPatternPartsCount(); j++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(resClass.getSqlColumn(varName, j));
                }
            }
        }

        if(!hasSelect)
            builder.append("1");


        builder.append(" FROM (");
        builder.append(left.translate());
        builder.append(") AS ");
        builder.append(leftTable);


        builder.append(" WHERE (SELECT 1 FROM (");
        builder.append(right.translate());
        builder.append(") AS ");
        builder.append(rightTable);
        builder.append(" WHERE ");
        generateWhereCondition(builder);
        builder.append(" LIMIT 1) IS NULL");


        return builder.toString();
    }


    private void generateWhereCondition(StringBuilder builder)
    {
        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());

        boolean hasWhere = false;
        boolean simpleCondition = false;

        for(UsedPairedVariable pair : pairs)
        {
            String var = pair.getName();
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                simpleCondition |= !leftVariable.canBeNull() && !rightVariable.canBeNull();

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


        if(!simpleCondition)
        {
            builder.append(" AND (");
            boolean restricted = false;

            for(UsedPairedVariable pair : pairs)
            {
                String var = pair.getName();
                UsedVariable leftVariable = pair.getLeftVariable();
                UsedVariable rightVariable = pair.getRightVariable();

                if(leftVariable != null && rightVariable != null)
                {
                    appendOr(builder, restricted);
                    restricted = true;

                    builder.append("(");

                    if(leftVariable.canBeNull())
                    {
                        boolean use = false;
                        builder.append("(");

                        for(ResourceClass resClass : leftVariable.getClasses())
                        {
                            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                            {
                                appendOr(builder, use);
                                use = true;

                                builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                                builder.append(" IS NOT NULL");
                            }
                        }

                        builder.append(")");

                        assert use;
                    }

                    if(leftVariable.canBeNull() && rightVariable.canBeNull())
                        builder.append(" AND ");

                    if(rightVariable.canBeNull())
                    {
                        boolean use = false;
                        builder.append("(");

                        for(ResourceClass resClass : rightVariable.getClasses())
                        {
                            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                            {
                                appendOr(builder, use);
                                use = true;

                                builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
                                builder.append(" IS NOT NULL");
                            }
                        }

                        builder.append(")");

                        assert use;
                    }

                    builder.append(")");
                }
            }

            builder.append(")");
        }
    }
}
