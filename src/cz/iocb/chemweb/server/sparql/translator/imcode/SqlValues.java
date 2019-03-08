package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    private final ArrayList<Pair<String, Set<ResourceClass>>> typedVariables;
    private final ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList;


    public SqlValues(UsedVariables usedVariables, ArrayList<Pair<String, Set<ResourceClass>>> typedVariables,
            ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList)
    {
        super(usedVariables, true);
        this.typedVariables = typedVariables;
        this.typedValuesList = typedValuesList;
    }


    @Override
    public SqlIntercode optimize(DatabaseSchema schema, HashSet<String> restrictions, boolean reduced)
    {
        return new SqlValues(variables.restrict(restrictions), typedVariables, typedValuesList);
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
