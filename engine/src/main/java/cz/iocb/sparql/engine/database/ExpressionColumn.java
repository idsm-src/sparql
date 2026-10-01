package cz.iocb.sparql.engine.database;

import java.util.Objects;



/**
 * Arbitrary SQL expression of a known type.
 */
public final class ExpressionColumn extends Column
{
    /**
     * Whether the expression may evaluate to NULL.
     */
    private final boolean canBeNull;


    /**
     * Creates the expression column with the given nullability. The type is not part of the identity of the column: two
     * columns of the same expression are equal regardless of it.
     *
     * @param value the SQL expression
     * @param type SQL type of the expression
     * @param canBeNull whether the value may be null
     */
    public ExpressionColumn(String value, SqlType type, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL expression
        super(value, Objects.requireNonNull(type, "type of expression " + value));
        this.canBeNull = canBeNull;
    }


    /**
     * Creates a possibly-null expression column.
     *
     * @param value the SQL expression
     * @param type SQL type of the expression
     */
    public ExpressionColumn(String value, SqlType type)
    {
        this(value, type, true);
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
    @Override
    public boolean canBeNull()
    {
        return canBeNull;
    }
}
