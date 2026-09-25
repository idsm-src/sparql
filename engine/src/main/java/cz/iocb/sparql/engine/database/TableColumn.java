package cz.iocb.sparql.engine.database;



/**
 * Named column of a table, rendered double-quoted.
 */
public final class TableColumn extends Column
{
    /**
     * Whether the values of the column may be NULL.
     */
    private final boolean canBeNull;


    /**
     * Creates the column reference from its unquoted name; the values may be NULL.
     *
     * @param value the column name
     */
    public TableColumn(String value)
    {
        this(value, true);
    }


    /**
     * Creates the column reference from its unquoted name with the knowledge whether its values may be NULL. The
     * knowledge is not part of the identity of the column: two references to the same column are equal regardless of
     * it.
     *
     * @param value the column name
     * @param canBeNull whether the values of the column may be NULL
     */
    public TableColumn(String value, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL column name
        super(value);
        this.canBeNull = canBeNull;
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

        return new ExpressionColumn(table + "." + this, canBeNull);
    }


    @Override
    public boolean canBeNull()
    {
        return canBeNull;
    }
}
