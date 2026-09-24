package cz.iocb.sparql.engine.database;

import java.util.List;



/**
 * Optionally schema-qualified table (or view) name, rendered double-quoted.
 */
public class Table
{
    /**
     * Schema, or null for the default search path.
     */
    private final String schema;

    /**
     * Name of the table.
     */
    private final String table;


    /**
     * Creates a schema-qualified reference.
     *
     * @param schema the schema name
     * @param table the table name
     */
    public Table(String schema, String table)
    {
        this.schema = schema;
        this.table = table;
    }


    /**
     * Creates an unqualified reference.
     *
     * @param table the table name
     */
    public Table(String table)
    {
        this.schema = null;
        this.table = table;
    }


    /**
     * Schema, or null when unqualified.
     *
     * @return schema, or null when unqualified
     */
    public String getSchema()
    {
        return schema;
    }


    /**
     * Name of the table without the schema.
     *
     * @return name of the table without the schema
     */
    public String getName()
    {
        return table;
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


    @Override
    public String toString()
    {
        if(schema != null)
            return "\"" + schema.replaceAll("\"", "\"\"") + "\".\"" + table.replaceAll("\"", "\"\"") + "\"";
        else
            return "\"" + table.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public int hashCode()
    {
        return table.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Table other = (Table) object;

        return (schema == other.schema || schema != null && schema.equals(other.schema)) && table.equals(other.table);
    }
}
