package cz.iocb.sparql.engine.database;

import static java.util.stream.Collectors.joining;
import java.util.List;
import java.util.Set;



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
            case ConstantColumn _ -> 0;
            case TableColumn _ -> 1;
            case ExpressionColumn _ -> 2;
            default -> Integer.MAX_VALUE;
        };
    }


    @Override
    public int compareTo(Column o)
    {
        int typeCompare = Integer.compare(order(this), order(o));
        return typeCompare != 0 ? typeCompare : toString().compareTo(o.toString());
    }


    public static Column coalesce(Set<? extends Column> cols)
    {
        List<? extends Column> list = cols.stream().sorted().toList();

        if(list.get(0) instanceof ConstantColumn || list.size() == 1)
            return list.get(0);

        return new ExpressionColumn(list.stream().map(Object::toString).collect(joining(",", "coalesce(", ")")));
    }
}
