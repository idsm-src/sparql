package cz.iocb.sparql.engine.database;



/**
 * Schema-qualified SQL function name, rendered double-quoted.
 */
public class Function
{
    /**
     * Schema of the function.
     */
    private final String schema;

    /**
     * Name of the function.
     */
    private final String function;


    /**
     * Creates the reference.
     *
     * @param schema the schema name
     * @param function name of the SQL function
     */
    public Function(String schema, String function)
    {
        this.schema = schema;
        this.function = function;
    }


    /**
     * Schema of the function.
     *
     * @return schema of the function
     */
    public String getSchema()
    {
        return schema;
    }


    /**
     * Name of the function without the schema.
     *
     * @return name of the function without the schema
     */
    public String getName()
    {
        return function;
    }


    @Override
    public String toString()
    {
        return "\"" + schema.replaceAll("\"", "\"\"") + "\".\"" + function.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public int hashCode()
    {
        return function.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Function other = (Function) object;

        return schema.equals(other.schema) && function.equals(other.function);
    }
}
