package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    final ArrayList<Pair<String, Set<ResourceClass>>> typedVariables;
    final ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList;


    SqlValues(UsedVariables usedVariables, ArrayList<Pair<String, Set<ResourceClass>>> typedVariables,
            ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList)
    {
        super(usedVariables, true);
        this.typedVariables = typedVariables;
        this.typedValuesList = typedValuesList;
    }


    public static SqlIntercode create(ArrayList<Pair<String, Set<ResourceClass>>> typedVariables,
            ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList)
    {
        if(typedValuesList.isEmpty())
            return SqlNoSolution.get();

        UsedVariables usedVariables = new UsedVariables();


        ArrayList<Pair<String, Set<ResourceClass>>> filteredTypedVariables = new ArrayList<>(typedVariables.size());
        boolean mask[] = new boolean[typedVariables.size()];

        for(int i = 0; i < typedVariables.size(); i++)
        {
            if(!typedVariables.get(i).getValue().isEmpty())
            {
                mask[i] = true;
                filteredTypedVariables.add(typedVariables.get(i));

                final int fi = i;
                boolean canBeNull = typedValuesList.size() > typedValuesList.stream()
                        .filter(l -> l.get(fi).getValue() != null).count();
                usedVariables.add(
                        new UsedVariable(typedVariables.get(i).getKey(), typedVariables.get(i).getValue(), canBeNull));
            }
        }


        ArrayList<ArrayList<Pair<Node, ResourceClass>>> filteredTypedValuesList = new ArrayList<>(
                typedValuesList.size());

        for(ArrayList<Pair<Node, ResourceClass>> values : typedValuesList)
        {
            ArrayList<Pair<Node, ResourceClass>> optimizedValues = new ArrayList<>(filteredTypedVariables.size());

            for(int i = 0; i < values.size(); i++)
                if(mask[i])
                    optimizedValues.add(values.get(i));

            filteredTypedValuesList.add(optimizedValues);
        }


        return new SqlValues(usedVariables, filteredTypedVariables, filteredTypedValuesList);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        ArrayList<Pair<String, Set<ResourceClass>>> optimizedTypedVariables = new ArrayList<>(typedVariables.size());
        boolean mask[] = new boolean[typedVariables.size()];

        for(int i = 0; i < typedVariables.size(); i++)
        {
            if(restrictions.contains(typedVariables.get(i).getKey()))
            {
                mask[i] = true;
                optimizedTypedVariables.add(typedVariables.get(i));
            }
        }


        ArrayList<ArrayList<Pair<Node, ResourceClass>>> optimizedTypedValuesList = new ArrayList<>(
                typedValuesList.size());

        for(ArrayList<Pair<Node, ResourceClass>> values : typedValuesList)
        {
            ArrayList<Pair<Node, ResourceClass>> optimizedValues = new ArrayList<>(optimizedTypedVariables.size());

            for(int i = 0; i < values.size(); i++)
                if(mask[i])
                    optimizedValues.add(values.get(i));

            optimizedTypedValuesList.add(optimizedValues);
        }


        return SqlValues.create(optimizedTypedVariables, optimizedTypedValuesList);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM (VALUES ");

        for(int i = 0; i < typedValuesList.size(); i++)
        {
            appendComma(builder, i > 0);
            builder.append("(");

            ArrayList<Pair<Node, ResourceClass>> valueList = typedValuesList.get(i);

            boolean hasValue = false;

            for(int j = 0; j < typedVariables.size(); j++)
            {
                Pair<String, Set<ResourceClass>> typedVariable = typedVariables.get(j);
                Pair<Node, ResourceClass> value = valueList.get(j);

                if(variables.get(typedVariable.getKey()) == null)
                    continue;

                if(!typedVariable.getValue().isEmpty())
                {
                    for(ResourceClass resClass : typedVariable.getValue())
                    {
                        if(resClass == value.getValue())
                        {
                            for(int k = 0; k < resClass.getPatternPartsCount(); k++)
                            {
                                appendComma(builder, hasValue);
                                hasValue = true;

                                builder.append(resClass.getPatternCode(value.getKey(), k));
                            }
                        }
                        else
                        {
                            for(int k = 0; k < resClass.getPatternPartsCount(); k++)
                            {
                                appendComma(builder, hasValue);
                                hasValue = true;

                                builder.append("null");
                            }
                        }
                    }
                }
                else
                {
                    appendComma(builder, hasValue);
                    hasValue = true;
                    builder.append("null");
                }
            }

            if(!hasValue)
                builder.append("null");

            builder.append(") ");
        }

        builder.append(") AS tab (");


        boolean hasValue = false;

        for(Pair<String, Set<ResourceClass>> typedVariable : typedVariables)
        {
            if(variables.get(typedVariable.getKey()) == null)
                continue;

            if(!typedVariable.getValue().isEmpty())
            {
                for(ResourceClass resClass : typedVariable.getValue())
                {
                    for(int k = 0; k < resClass.getPatternPartsCount(); k++)
                    {
                        appendComma(builder, hasValue);
                        hasValue = true;

                        builder.append(resClass.getSqlColumn(typedVariable.getKey(), k));
                    }
                }
            }
            else
            {
                appendComma(builder, hasValue);
                hasValue = true;
                builder.append("\"null\"");
            }
        }

        if(!hasValue)
            builder.append("\"#null\"");

        builder.append(")");

        return builder.toString();
    }
}
