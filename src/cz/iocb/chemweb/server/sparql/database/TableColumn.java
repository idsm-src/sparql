package cz.iocb.chemweb.server.sparql.database;



public class TableColumn extends Column
{
    public TableColumn(String value)
    {
        super(value);
    }


    @Override
    public String getCode()
    {
        return "\"" + value.replaceAll("\"", "\"\"") + "\"";
    }
}
