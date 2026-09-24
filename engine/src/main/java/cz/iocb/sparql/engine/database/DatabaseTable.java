package cz.iocb.sparql.engine.database;



/**
 * Schema-qualified name of a table (or view) stored in the database, rendered {@code "schema"."table"}. The schema is
 * mandatory, so the reference does not depend on the search path of the connection.
 */
public final class DatabaseTable extends SourceTable
{
    /**
     * Schema of the table.
     */
    private final String schema;


    /**
     * Creates the reference.
     *
     * @param schema the schema name
     * @param table the table name
     * @throws IllegalArgumentException if the schema is null
     */
    public DatabaseTable(String schema, String table)
    {
        super(table);

        if(schema == null)
            throw new IllegalArgumentException("database table " + table + " must be schema-qualified");

        this.schema = schema;
    }


    /**
     * Schema of the table.
     *
     * @return schema of the table
     */
    public String getSchema()
    {
        return schema;
    }


    @Override
    public String toString()
    {
        return quote(schema) + "." + quote(getName());
    }


    @Override
    public boolean equals(Object object)
    {
        if(!super.equals(object))
            return false;

        DatabaseTable other = (DatabaseTable) object;

        return schema.equals(other.schema);
    }
}
