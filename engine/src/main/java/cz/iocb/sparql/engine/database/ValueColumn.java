package cz.iocb.sparql.engine.database;

import java.util.Objects;



/**
 * The typed constant {@code 'literal'::type} of a non-null value.
 */
public final class ValueColumn extends ConstantColumn
{
    /**
     * Unquoted literal value.
     */
    private final String literal;


    /**
     * Creates the constant.
     *
     * @param literal the literal value
     * @param type the SQL type
     */
    public ValueColumn(String literal, SqlType type)
    {
        super("'" + Objects.requireNonNull(literal).replaceAll("'", "''") + "'::" + type, type);
        this.literal = literal;
    }


    /**
     * The unquoted literal value.
     *
     * @return the unquoted literal value
     */
    public String getValue()
    {
        return literal;
    }


    @Override
    public boolean canBeNull()
    {
        return false;
    }
}
