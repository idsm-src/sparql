package cz.iocb.chemweb.server.sparql.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



public class ConstructResult extends Result
{
    private final ArrayList<RdfNode[]> templates;
    private final SelectResult subresult;
    private int templateIdx;
    private int groupIdx = 0;


    public ConstructResult(ArrayList<RdfNode[]> templates, ResultSet executeQuery) throws SQLException
    {
        super(ResultType.CONSTRUCT);

        for(String name : new String[] { "subject", "predicate", "object" })
        {
            varNames.put(name, heads.size());
            heads.add(name);
        }

        this.templates = templates;
        this.subresult = new SelectResult(ResultType.SELECT, executeQuery);
        this.rowData = new RdfNode[heads.size()];
        this.templateIdx = templates.size();
    }


    @Override
    public boolean next() throws SQLException
    {
        if(templates.size() == 0)
            return false;

        if(templateIdx == templates.size())
        {
            if(subresult.next() == false)
                return false;

            groupIdx++;
            templateIdx = 0;
        }


        RdfNode[] template = templates.get(templateIdx++);

        for(int i = 0; i < 3; i++)
        {
            if(template[i] instanceof BNode)
                rowData[i] = new BNode("T" + template[i].getValue() + "#" + groupIdx);
            else if(template[i] instanceof VariableNode)
                rowData[i] = subresult.get(template[i].getValue());
            else
                rowData[i] = template[i];

            if(i < 2 && rowData[i] instanceof LiteralNode)
                return next();
        }

        return true;
    }


    @Override
    public List<String> getWarnings() throws SQLException
    {
        return subresult.getWarnings();
    }


    @Override
    public void close() throws SQLException
    {
        subresult.close();
    }
}
