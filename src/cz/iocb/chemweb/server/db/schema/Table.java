package cz.iocb.chemweb.server.db.schema;

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

        return value.equals(other.value);
    }
}
