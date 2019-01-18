package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.TranslatedSegment;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlExists extends SqlExpressionIntercode
{
    private static final String table = "tab";
    private final boolean negated;
    private final TranslatedSegment pattern;
    private final VariableAccessor variableAccessor;


    protected SqlExists(boolean negated, TranslatedSegment pattern, VariableAccessor variableAccessor)
    {
        super(asSet(xsdBoolean), false);
        this.negated = negated;
        this.pattern = pattern;
        this.variableAccessor = variableAccessor;
    }


    public static SqlExpressionIntercode create(boolean negated, TranslatedSegment pattern,
            VariableAccessor variableAccessor)
    {
        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(pattern.getIntercode().getVariables(),
                variableAccessor.getUsedVariables());

        for(UsedPairedVariable pair : pairs)
        {
            if(pair.getLeftVariable() != null && pair.getRightVariable() != null)
            {
                boolean isJoinable = false;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if((pairedClass.getLeftClass() != null || pair.getLeftVariable().canBeNull())
                            && (pairedClass.getRightClass() != null || pair.getRightVariable().canBeNull()))

                        isJoinable = true;
                }

                if(!isJoinable)
                    return negated ? SqlEffectiveBooleanValue.trueValue : SqlEffectiveBooleanValue.falseValue;
            }
        }

        return new SqlExists(negated, pattern, variableAccessor);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return create(negated, pattern, variableAccessor);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("(NOT ");

        builder.append("COALESCE((SELECT true FROM (");

        builder.append(pattern.getIntercode().translate());

        builder.append(") AS ");
        builder.append(table);
        builder.append(" WHERE ");

        boolean hasWhere = false;

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(pattern.getIntercode().getVariables(),
                variableAccessor.getUsedVariables());

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

                            builder.append(table).append('.').append(resClass.getSqlColumn(var, i));
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

                            builder.append(resClass.getSqlColumn(var, i));
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

                                builder.append(table).append('.').append(resClass.getSqlColumn(var, i));
                                builder.append(" = ");
                                builder.append(resClass.getSqlColumn(var, i));
                            }
                        }
                        else
                        {
                            ResourceClass resClass = pairedClass.getLeftClass().getGeneralClass();

                            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);

                                builder.append(
                                        pairedClass.getLeftClass().getGeneralisedPatternCode(table, var, i, false));
                                builder.append(" = ");
                                builder.append(
                                        pairedClass.getRightClass().getGeneralisedPatternCode(null, var, i, false));
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

        builder.append(" LIMIT 1), false)");

        if(negated)
            builder.append(")");

        return builder.toString();
    }
}
