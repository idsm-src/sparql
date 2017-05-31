package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlQuery extends SqlIntercode
{
    private final ArrayList<String> selectedVariables;
    private final SqlIntercode child;

    public SqlQuery(ArrayList<String> selectedVariables, SqlIntercode child)
    {
        super(child.variables);
        this.selectedVariables = selectedVariables;
        this.child = child;
    }


    @Override
    public String translate()
    {
        UsedVariables variables = child.getVariables();
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(String variableName : selectedVariables)
        {
            UsedVariable variable = variables.get(variableName);

            if(variable == null)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append("NULL AS \"");
                builder.append(variableName);
                builder.append('#');
                builder.append(ResourceClass.nullTag);
                builder.append('"');
            }
            else
            {
                List<ResourceClass> classes = variable.getClasses();
                List<ResourceClass> iriClasses = new ArrayList<ResourceClass>();
                List<ResourceClass> literalClasses = new ArrayList<ResourceClass>();

                for(ResourceClass resClass : classes)
                {
                    if(resClass instanceof IriClass)
                        iriClasses.add(resClass);
                    else
                        literalClasses.add(resClass);
                }

                if(!iriClasses.isEmpty())
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    if(iriClasses.size() > 1)
                        builder.append("COALESCE(");

                    for(int i = 0; i < iriClasses.size(); i++)
                    {
                        appendComma(builder, i > 0);

                        ResourceClass iriClass = iriClasses.get(i);
                        builder.append(iriClass.getSparqlValue(variableName));
                    }

                    if(iriClasses.size() > 1)
                        builder.append(")");

                    builder.append(" AS \"");
                    builder.append(variableName);
                    builder.append('#');
                    builder.append(IriClass.iriTag);
                    builder.append('"');
                }

                for(ResourceClass literalClass : literalClasses)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(literalClass.getSparqlValue(variable.getName()));
                }
            }
        }

        if(!hasSelect)
        {
            builder.append("1 AS \"*#");
            builder.append(ResourceClass.nullTag);
            builder.append('"');
        }


        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(") AS tab");


        return builder.toString();
    }
}
