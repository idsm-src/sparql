package cz.iocb.sparql.engine.database;

import static java.util.stream.Collectors.joining;
import java.util.List;
import java.util.Set;



/**
 * A column reference, constant or SQL expression usable in generated SQL. The natural ordering puts constants first,
 * then table columns, then expressions.
 */
public abstract class Column implements Comparable<Column>
{
    /**
     * SQL text of the column: the bare name, the constant or the expression.
     */
    protected final String value;


    /**
     * Creates the column with its SQL text.
     *
     * @param value SQL text of the column
     */
    protected Column(String value)
    {
        this.value = value;
    }


    /**
     * SQL text of the column without quoting.
     *
     * @return SQL text of the column without quoting
     */
    public String getName()
    {
        return value;
    }


    /**
     * This column qualified by the table (or alias); constants are returned unchanged, expressions cannot be qualified.
     *
     * @param table the table
     * @return this column qualified by the table (or alias); constants are returned unchanged, expressions cannot be
     *         qualified
     */
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


    /**
     * Rank of the column kind for ordering: constants, then table columns, then expressions.
     *
     * @param column the column
     * @return rank of the column kind for ordering: constants, then table columns, then expressions
     */
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


    /**
     * Column yielding the first non-null of the given columns: a constant if one is present, the only column, or a
     * {@code COALESCE} expression.
     *
     * @param cols the columns
     * @return column yielding the first non-null of the given columns: a constant if one is present, the only column,
     *         or a {@code COALESCE} expression
     */
    public static Column coalesce(Set<? extends Column> cols)
    {
        List<? extends Column> list = cols.stream().sorted().toList();

        if(list.get(0) instanceof ConstantColumn || list.size() == 1)
            return list.get(0);

        return new ExpressionColumn(list.stream().map(Object::toString).collect(joining(",", "COALESCE(", ")")));
    }
}
