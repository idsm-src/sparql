package cz.iocb.chemweb.server.sparql.translator.imcode;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;



public class SqlFilter extends SqlIntercode
{
    private final SqlIntercode child;
    private final String condition;


    public SqlFilter(SqlIntercode child, String condition)
    {
        super(child.variables);
        this.child = child;
        this.condition = condition;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(UsedVariable variable : child.variables.getValues())
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

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        builder.append(" WHERE ");
        builder.append(condition);

        return builder.toString();
    }
}
