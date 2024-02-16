package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlValues;



public class ValuesResultHandler extends ResultHandler
{
    private final List<String> variables;
    private final HashMap<String, Integer> indexes;

    private ArrayList<List<Node>> results = new ArrayList<List<Node>>();


    public ValuesResultHandler(Collection<String> vars)
    {
        variables = new ArrayList<String>(vars.size());
        indexes = new HashMap<String, Integer>();

        for(String var : vars)
        {
            variables.add(var);
            indexes.put(var, indexes.size());
        }
    }


    @Override
    public void add(Map<String, Node> row)
    {
        List<Node> result = new ArrayList<Node>(variables.size());

        for(int i = 0; i < variables.size(); i++)
            result.add(null);

        for(Entry<String, Node> entry : row.entrySet())
            result.set(indexes.get(entry.getKey()), entry.getValue());

        results.add(result);
    }


    @Override
    public SqlIntercode get()
    {
        return SqlValues.create(variables, results);
    }


    @Override
    public int size()
    {
        return results.size();
    }
}
