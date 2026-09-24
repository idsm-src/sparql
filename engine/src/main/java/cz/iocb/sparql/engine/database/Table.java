package cz.iocb.sparql.engine.database;

import java.util.List;



/**
 * Name of a table-like relation appearing in the generated SQL, rendered double-quoted. A {@link SourceTable} is a
 * relation the mappings read from (a {@link DatabaseTable} of the database or a {@link VirtualTable} defined by the
 * configuration), whereas a {@link AliasTable} only names a subquery inside the generated statement.
 */
public abstract class Table
{
    /**
     * Name of the table.
     */
    private final String name;


    /**
     * Creates the reference.
     *
     * @param name the table name
     */
    protected Table(String name)
    {
        this.name = name;
    }


    /**
     * Name of the table without any qualification.
     *
     * @return name of the table without any qualification
     */
    public final String getName()
    {
        return name;
    }


    /**
     * Qualifies the columns by the table; a null table leaves them unchanged.
     *
     * @param table the table
     * @param columns the columns
     * @return the qualified columns
     */
    public static List<Column> toTableColumns(Table table, List<Column> columns)
    {
        if(table == null)
            return columns;

        if(columns == null)
            return null;

        return columns.stream().map(c -> c.fromTable(table)).toList();
    }


    /**
     * Renders the identifier double-quoted, doubling the embedded quotes.
     *
     * @param identifier the identifier
     * @return the quoted identifier
     */
    protected static String quote(String identifier)
    {
        return "\"" + identifier.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public String toString()
    {
        return quote(name);
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Table other = (Table) object;

        return name.equals(other.name);
    }
}
