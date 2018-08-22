package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.Pair;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    private final ArrayList<Pair<String, ArrayList<PatternResourceClass>>> typedVariables;
    private final ArrayList<ArrayList<Pair<Node, PatternResourceClass>>> typedValuesList;


    public SqlValues(UsedVariables usedVariables,
            ArrayList<Pair<String, ArrayList<PatternResourceClass>>> typedVariables,
            ArrayList<ArrayList<Pair<Node, PatternResourceClass>>> typedValuesList)
    {
        super(usedVariables);
        this.typedVariables = typedVariables;
        this.typedValuesList = typedValuesList;
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

            ArrayList<Pair<Node, PatternResourceClass>> valueList = typedValuesList.get(i);

            boolean hasValue = false;
            for(int j = 0; j < typedVariables.size(); j++)
            {
                Pair<String, ArrayList<PatternResourceClass>> typedVariable = typedVariables.get(j);
                Pair<Node, PatternResourceClass> value = valueList.get(j);

                for(PatternResourceClass resClass : typedVariable.getValue())
                {
                    if(resClass == value.getValue())
                    {
                        for(int k = 0; k < resClass.getPartsCount(); k++)
                        {
                            appendComma(builder, hasValue);
                            hasValue = true;

                            builder.append(resClass.getSqlValue(value.getKey(), k));
                        }
                    }
                    else
                    {
                        for(int k = 0; k < resClass.getPartsCount(); k++)
                        {
                            appendComma(builder, hasValue);
                            hasValue = true;

                            builder.append("null");
                        }
                    }
                }
            }

            builder.append(") ");
        }

        builder.append(") AS tab (");


        boolean hasValue = false;

        for(Pair<String, ArrayList<PatternResourceClass>> typedVariable : typedVariables)
        {
            for(PatternResourceClass resClass : typedVariable.getValue())
            {
                for(int k = 0; k < resClass.getPartsCount(); k++)
                {
                    appendComma(builder, hasValue);
                    hasValue = true;

                    builder.append(resClass.getSqlColumn(typedVariable.getKey(), k));
                }
            }
        }

        builder.append(")");

        return builder.toString();
    }
}
