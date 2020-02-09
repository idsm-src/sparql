package cz.iocb.chemweb.server.sparql.database;



public class ExpressionColumn extends Column
{
    public ExpressionColumn(String value)
    {
        super(value);
    }


    @Override
    public String getCode()
    {
        return value; //FIXME: add escaping if needed
    }
}
