package cz.iocb.sparql.engine.database;



/**
 * Typed SQL constant {@code 'literal'::type} (or {@code NULL::type}); the type is written under its canonical name.
 */
public class ConstantColumn extends Column
{
    /**
     * Unquoted literal value; null for NULL.
     */
    private final String literal;

    /**
     * SQL type of the constant.
     */
    private final SqlType type;

    /**
     * Creates the constant.
     *
     * @param literal the literal value, null for NULL
     * @param type the SQL type
     */
    public ConstantColumn(String literal, SqlType type)
    {
        super((literal == null ? "NULL" : "'" + literal.replaceAll("'", "''") + "'") + "::" + type);

        this.literal = literal;
        this.type = type;
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


    /**
     * The unquoted literal value; null for a NULL constant.
     *
     * @return the unquoted literal value; null for a NULL constant
     */
    public String getValue()
    {
        return literal;
    }


    /**
     * SQL type of the constant.
     *
     * @return SQL type of the constant
     */
    public SqlType getType()
    {
        return type;
    }


    @Override
    public boolean canBeNull()
    {
        return literal == null;
    }
}
