package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
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
                builder.append(ResultTag.RDFTERM.getTag());
                builder.append('"');
            }
            else
            {
                List<ResourceClass> classes = variable.getClasses();

                LinkedHashMap<List<ResultTag>, List<ResourceClass>> resultClasses = new LinkedHashMap<>();

                for(ResourceClass resClass : classes)
                {
                    List<ResourceClass> list = resultClasses.get(resClass.getResultTags());

                    if(list == null)
                    {
                        list = new ArrayList<ResourceClass>();
                        resultClasses.put(resClass.getResultTags(), list);
                    }

                    list.add(resClass);
                }


                for(Entry<List<ResultTag>, List<ResourceClass>> entry : resultClasses.entrySet())
                {
                    List<ResultTag> tags = entry.getKey();
                    List<ResourceClass> resClasses = entry.getValue();

                    for(int part = 0; part < tags.size(); part++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(resClasses.size() > 1)
                            builder.append("COALESCE(");

                        for(int i = 0; i < resClasses.size(); i++)
                        {
                            appendComma(builder, i > 0);

                            ResourceClass resClass = resClasses.get(i);
                            builder.append(resClass.getResultValue(variableName, part));
                        }

                        if(resClasses.size() > 1)
                            builder.append(")");

                        builder.append(" AS \"");

                        builder.append(variableName);
                        builder.append('#');
                        builder.append(tags.get(part).getTag());
                        builder.append('"');
                    }
                }
            }
        }

        if(!hasSelect)
        {
            builder.append("1 AS \"*#");
            builder.append(ResultTag.RDFTERM.getTag());
            builder.append('"');
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(") AS tab");

        return builder.toString();
    }
}
