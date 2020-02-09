package cz.iocb.chemweb.server.sparql.database;



public class ConstantColumn extends Column
{
    public ConstantColumn(String value)
    {
        super(value);
    }


    @Override
    public String getCode()
    {
        return value; //FIXME: add escaping if needed
    }
}
