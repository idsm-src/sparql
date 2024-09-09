package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultTag;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlQuery extends SqlIntercode
{
    private final SqlIntercode child;
    private final Collection<String> selectedVariables;


    public SqlQuery(Collection<String> selectedVariables, SqlIntercode child)
    {
        super(new UsedVariables(child.variables), child.isDeterministic());

        this.child = child;
        this.selectedVariables = selectedVariables;
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        throw new UnsupportedOperationException();
    }


    public SqlIntercode optimize(Request request)
    {
        Set<String> restrictions = new HashSet<String>(selectedVariables);
        return new SqlQuery(selectedVariables, child.optimize(request, restrictions, false));
    }


    @Override
    public String translate(Request request)
    {
        UsedVariables variables = child.getVariables();
        StringBuilder builder = new StringBuilder();

        if(child instanceof SqlSelect select && select.getChild() instanceof SqlUnion union
                && select.getDistinctVariables().isEmpty() && select.getOrderByVariables().isEmpty())
        {
            for(int i = 0; i < union.getChilds().size(); i++)
            {
                if(i > 0)
                    builder.append(" UNION ALL ");

                builder.append(translateChild(request, selectedVariables, variables, union.getChilds().get(i)));
            }

            if(select.getLimit() != null)
                builder.append(" LIMIT ").append(select.getLimit().toString());

            if(select.getOffset() != null)
                builder.append(" OFFSET ").append(select.getOffset().toString());
        }
        else if(child instanceof SqlUnion union)
        {
            for(int i = 0; i < union.getChilds().size(); i++)
            {
                if(i > 0)
                    builder.append(" UNION ALL ");

                builder.append(translateChild(request, selectedVariables, variables, union.getChilds().get(i)));
            }
        }
        else
        {
            builder.append(translateChild(request, selectedVariables, variables, child));
        }

        return builder.toString();
    }


    public static String translateChild(Request request, Collection<String> selectedVariables, UsedVariables variables,
            SqlIntercode child)
    {
        UsedVariables childVariables = child.getVariables();
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(String variableName : selectedVariables)
        {
            UsedVariable variable = variables.get(variableName);
            UsedVariable childVariable = childVariables.get(variableName);

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
                    List<ResourceClass> fullClasses = entry.getValue();

                    Set<ResourceClass> childClasses = childVariable != null ? childVariable.getClasses() :
                            new HashSet<ResourceClass>();

                    List<ResourceClass> resClasses = fullClasses.stream().filter(c -> childClasses.contains(c))
                            .collect(toList());

                    for(int part = 0; part < tags.size(); part++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(resClasses.size() == 0)
                        {
                            builder.append("NULL::" + tags.get(part).getSqlType());
                        }
                        else
                        {
                            if(resClasses.size() > 1)
                                builder.append("coalesce(");

                            for(int i = 0; i < resClasses.size(); i++)
                            {
                                appendComma(builder, i > 0);

                                ResourceClass resClass = resClasses.get(i);
                                builder.append(resClass.toResult(childVariable.getMapping(resClass)).get(part));
                            }

                            if(resClasses.size() > 1)
                                builder.append(")");
                        }

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
        builder.append(child.translate(request));
        builder.append(") AS tab");

        return builder.toString();
    }


    public SqlIntercode getChild()
    {
        return child;
    }


    public Collection<String> getSelectedVariables()
    {
        return selectedVariables;
    }
}
