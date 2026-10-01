package cz.iocb.sparql.engine.database;

import java.util.Objects;



/**
 * Typed SQL constant: a {@link ValueColumn} {@code 'literal'::type} or a {@link NullColumn} {@code NULL::type}; the
 * type is written under its canonical name. Constants are not projected by the generated queries and are compared at
 * translation time by their text.
 */
public abstract sealed class ConstantColumn extends Column permits NullColumn, ValueColumn
{
    /**
     * Creates the constant.
     *
     * @param value the SQL text of the constant
     * @param type the SQL type
     */
    protected ConstantColumn(String value, SqlType type)
    {
        super(value, Objects.requireNonNull(type, "type of constant " + value));
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
