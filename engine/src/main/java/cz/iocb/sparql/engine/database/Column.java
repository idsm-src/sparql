package cz.iocb.sparql.engine.database;

public abstract class Column implements Comparable<Column>
{
    protected final String value;


    protected Column(String value)
    {
        this.value = value;
    }


    public String getName()
    {
        return value;
    }


    public abstract Column fromTable(Table table);


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Column other = (Column) object;

        return value.equals(other.value);
    }


    private static int order(Column column)
    {
        return switch(column)
        {
            case ConstantColumn c -> 0;
            case TableColumn c -> 1;
            case ExpressionColumn c -> 2;
            default -> Integer.MAX_VALUE;
        };
    }


    @Override
    public int compareTo(Column o)
    {
        int typeCompare = Integer.compare(order(this), order(o));
        return typeCompare != 0 ? typeCompare : toString().compareTo(o.toString());
    }
}
