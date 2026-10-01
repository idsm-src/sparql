package cz.iocb.sparql.engine.database;



/**
 * Named column of a table, rendered double-quoted. The type comes from the database schema, from the resource class of
 * the mapped term or from the column the reference stands for (see {@code SparqlDatabaseConfiguration#getColumns}).
 */
public final class TableColumn extends Column
{
    /**
     * Whether the values of the column may be NULL.
     */
    private final boolean canBeNull;


    /**
     * Creates the column reference from its unquoted name and type; the values may be NULL.
     *
     * @param value the column name
     * @param type SQL type of the column
     */
    public TableColumn(String value, SqlType type)
    {
        this(value, type, true);
    }


    /**
     * Creates the column reference from its unquoted name and type with the knowledge whether its values may be NULL.
     * Neither the type nor the knowledge is part of the identity of the column: two references to the same column are
     * equal regardless of them.
     *
     * @param value the column name
     * @param type SQL type of the column
     * @param canBeNull whether the values of the column may be NULL
     */
    public TableColumn(String value, SqlType type, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL column name
        super(value, type);
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

        return new ExpressionColumn(table + "." + this, type, canBeNull);
    }


    @Override
    public boolean canBeNull()
    {
        return canBeNull;
    }
}
