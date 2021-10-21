package cz.iocb.chemweb.server.sparql.database;



public class ConstantColumn extends Column
{
    public ConstantColumn(String value)
    {
        //TODO: check whether the parameter is a valid and normalized SQL constant value
        super(value);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        return this;
    }
}
