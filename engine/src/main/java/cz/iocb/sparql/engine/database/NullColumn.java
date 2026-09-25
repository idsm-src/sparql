package cz.iocb.sparql.engine.database;



/**
 * The typed constant {@code NULL::type}: the representation of an absent value, used to pad the columns of a class a
 * variable does not take and as the value of an optional column of a term that lacks the corresponding part.
 */
public final class NullColumn extends ConstantColumn
{
    /**
     * Creates the NULL constant of the given type.
     *
     * @param type the SQL type
     */
    public NullColumn(SqlType type)
    {
        super("NULL::" + type, type);
    }


    @Override
    public boolean canBeNull()
    {
        return true;
    }
}
