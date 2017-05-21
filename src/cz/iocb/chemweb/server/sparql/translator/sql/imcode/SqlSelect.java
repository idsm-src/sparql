package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import java.math.BigInteger;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private boolean distinct = false;
    private BigInteger limit = null;
    private BigInteger offset = null;


    public SqlSelect(UsedVariables variables, SqlIntercode child)
    {
        super(variables);
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

        for(UsedVariable variable : variables.getValues())
        {
            for(ResourceClass resClass : variable.getClasses())
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
