package cz.iocb.chemweb.server.sparql.database;



public abstract class Column
{
    private final String value;


    protected Column(String value)
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

        Column other = (Column) obj;

        return value.equals(other.value);
    }
}
