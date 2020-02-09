package cz.iocb.chemweb.server.sparql.database;



public class Function
{
    private final String schema;
    private final String function;


    public Function(String schema, String function)
    {
        this.schema = schema;
        this.function = function;
    }


    public String getSchema()
    {
        return schema;
    }


    public String getName()
    {
        return function;
    }


    public String getCode()
    {
        return "\"" + schema.replaceAll("\"", "\"\"") + "\".\"" + function.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public int hashCode()
    {
        return function.hashCode();
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || getClass() != obj.getClass())
            return false;

        Function other = (Function) obj;

        return schema.equals(other.schema) && function.equals(other.function);
    }
}
