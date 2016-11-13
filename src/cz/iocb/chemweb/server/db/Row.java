package cz.iocb.chemweb.server.db;

import java.util.HashMap;



public class Row
{
    private final HashMap<String, Integer> columnNames;
    private final RdfNode[] rowData;


    public Row(HashMap<String, Integer> columnNames, RdfNode[] rowData)
    {
        this.columnNames = columnNames;
        this.rowData = rowData;
    }


    public RdfNode get(String name)
    {
        Integer idx = columnNames.get(name.toUpperCase());

        if(idx == null)
            return null;

        return rowData[idx];
    }


    public RdfNode[] getRdfNodes()
    {
        return rowData;
    }
}
