package cz.iocb.sparql.engine.database;



/**
 * Arbitrary SQL expression.
 */
public class ExpressionColumn extends Column
{
    /**
     * Whether the expression may evaluate to NULL.
     */
    private final boolean canBeNull;


    /**
     * Creates the expression column with the given nullability.
     *
     * @param value the SQL expression
     * @param canBeNull whether the value may be null
     */
    public ExpressionColumn(String value, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL expression
        super(value);
        this.canBeNull = canBeNull;
    }


    /**
     * Creates a possibly-null expression column.
     *
     * @param value the SQL expression
     */
    public ExpressionColumn(String value)
    {
        this(value, true);
    }


    /**
     * Creates a possibly-null expression column from a format string and arguments.
     *
     * @param format the format string
     * @param args the format arguments
     */
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


    /**
     * True if the expression may evaluate to NULL.
     *
     * @return true if the expression may evaluate to NULL, false otherwise
     */
    public boolean canBeNull()
    {
        return canBeNull;
    }
}
