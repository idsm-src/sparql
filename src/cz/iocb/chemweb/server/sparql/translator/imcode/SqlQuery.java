package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlQuery extends SqlIntercode
{
    private final Collection<String> selectedVariables;
    private final SqlIntercode child;
    private int offset;
    private int limit;


    public SqlQuery(Collection<String> selectedVariables, SqlIntercode child)
    {
        super(child.variables, child.isDeterministic());
        this.selectedVariables = selectedVariables;
        this.child = child;
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        throw new UnsupportedOperationException();
    }


    public SqlIntercode optimize(Request request)
    {
        HashSet<String> restrictions = new HashSet<String>(selectedVariables);
        return new SqlQuery(selectedVariables, child.optimize(request, restrictions, false));
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

            if(variable == null || variable.getClasses().isEmpty())
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append("NULL AS \"");
                builder.append(variableName.replaceFirst("^@", ""));
                builder.append('#');
                builder.append(ResultTag.NULL.getTag());
                builder.append('"');
            }
            else
            {
                Set<ResourceClass> classes = variable.getClasses();

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
                            builder.append(resClass.getResultCode(variableName, part));
                        }

                        if(resClasses.size() > 1)
                            builder.append(")");

                        builder.append(" AS \"");

                        builder.append(variableName.replaceFirst("^@", ""));
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
            builder.append(ResultTag.NULL.getTag());
            builder.append('"');
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(") AS tab");

        if(offset > 0)
        {
            builder.append(" OFFSET ");
            builder.append(offset);
        }

        if(offset >= 0)
        {
            builder.append(" LIMIT ");
            builder.append(limit);
        }

        return builder.toString();
    }


    public void setOffset(int offset)
    {
        this.offset = offset;
    }


    public void setLimit(int limit)
    {
        this.limit = limit;
    }
}
