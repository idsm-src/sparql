package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.math.BigInteger;
import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final ArrayList<String> selectedVariables;
    private boolean distinct = false;
    private BigInteger limit = null;
    private BigInteger offset = null;


    public SqlSelect(ArrayList<String> selectedVariables, UsedVariables variables, SqlIntercode child)
    {
        super(variables);
        this.selectedVariables = selectedVariables;
        this.child = child;
    }


    public final void setDistinct(boolean distinct)
    {
        this.distinct = distinct;
    }


    public final void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }


    public final void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(distinct)
            builder.append("DISTINCT ");


        boolean hasSelect = false;

        for(String variableName : selectedVariables)
        {
            UsedVariable variable = variables.get(variableName);

            if(variable == null)
                continue;

            for(PatternResourceClass resClass : variable.getClasses())
            {
                for(int i = 0; i < resClass.getPartsCount(); i++)
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
        builder.append(child.translate());
        builder.append(" ) AS tab");


        if(limit != null)
        {
            builder.append(" LIMIT ");
            builder.append(limit.toString());
        }


        if(offset != null)
        {
            builder.append(" OFFSET ");
            builder.append(offset.toString());
        }


        return builder.toString();
    }
}
