package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public class SqlMinus extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final SqlIntercode left;
    private final SqlIntercode right;


    protected SqlMinus(SqlIntercode left, SqlIntercode right)
    {
        super(left.getVariables());
        this.left = left;
        this.right = right;
    }


    public static SqlIntercode minus(SqlIntercode left, SqlIntercode right)
    {
        boolean shareVariables = false;

        for(UsedVariable variable : right.getVariables().getValues())
            if(left.getVariables().get(variable.getName()) != null)
                shareVariables = true;

        if(shareVariables == false)
            return left;

        return new SqlMinus(left, right);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(UsedVariable variable : left.getVariables().getValues())
        {
            String varName = variable.getName();

            for(PatternResourceClass resClass : variable.getClasses())
            {
                for(int j = 0; j < resClass.getPartsCount(); j++)
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

                    for(PatternResourceClass resClass : leftVariable.getClasses())
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

                    for(PatternResourceClass resClass : rightVariable.getClasses())
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
                        PatternResourceClass resClass = pairedClass.getLeftClass();

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

                        for(PatternResourceClass resClass : leftVariable.getClasses())
                        {
                            for(int i = 0; i < resClass.getPartsCount(); i++)
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

                        for(PatternResourceClass resClass : rightVariable.getClasses())
                        {
                            for(int i = 0; i < resClass.getPartsCount(); i++)
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
