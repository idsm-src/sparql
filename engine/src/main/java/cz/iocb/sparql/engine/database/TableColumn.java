package cz.iocb.sparql.engine.database;



/**
 * Named column of a table, rendered double-quoted.
 */
public class TableColumn extends Column
{
    /**
     * Creates the column reference from its unquoted name.
     *
     * @param value the column name
     */
    public TableColumn(String value)
    {
        //TODO: check whether the parameter is a valid SQL column name
        super(value);
    }


    @Override
    public String toString()
    {
        return "\"" + value + "\"";
    }


    @Override
    public Column fromTable(Table table)
    {
        if(table == null)
            return this;

        return new ExpressionColumn(table + "." + this);
    }
}
