package cz.iocb.chemweb.server.sparql.database;



public class Table
{
    private final String value;


    public Table(String value)
    {
        this.value = value;
    }


    public String getName()
    {
        return value;
    }


    public String getCode()
    {
        return value;
    }


    @Override
    public int hashCode()
    {
        if(value == null)
            return 0;

        return value.hashCode();
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || getClass() != obj.getClass())
            return false;

        Table other = (Table) obj;

        return value == null && other.value == null || value != null && value.equals(other.value);
    }
}
