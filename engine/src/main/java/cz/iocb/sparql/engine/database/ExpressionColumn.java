package cz.iocb.sparql.engine.database;



public class ExpressionColumn extends Column
{
    private final boolean canBeNull;


    public ExpressionColumn(String value, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL expression
        super(value);
        this.canBeNull = canBeNull;
    }


    public ExpressionColumn(String value)
    {
        this(value, true);
    }


    public ExpressionColumn(String format, Object... args)
    {
        this(String.format(format, args), true);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        if(table == null)
            return this;

        throw new UnsupportedOperationException();
    }


    public boolean canBeNull()
    {
        return canBeNull;
    }
}
