package cz.iocb.sparql.engine.database;



/**
 * Typed SQL constant {@code 'literal'::type} (or {@code NULL::type}); the type is normalised to its PostgreSQL internal
 * name.
 */
public class ConstantColumn extends Column
{
    /**
     * Unquoted literal value; null for NULL.
     */
    private final String literal;

    /**
     * Creates the constant; the type is normalised to its PostgreSQL internal name.
     *
     * @param literal the literal value, null for NULL
     * @param type the SQL type
     */
    public ConstantColumn(String literal, String type)
    {
        super((literal == null ? "NULL" : "'" + literal.replaceAll("'", "''") + "'") + "::" + normalizeSqlType(type));

        this.literal = literal;
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        return this;
    }


    /**
     * The unquoted literal value; null for a NULL constant.
     *
     * @return the unquoted literal value; null for a NULL constant
     */
    public String getValue()
    {
        return literal;
    }


    /**
     * Maps SQL standard type names to the PostgreSQL internal names used in generated code.
     *
     * @param type the SQL type
     * @return the PostgreSQL internal type name
     */
    private static String normalizeSqlType(String type)
    {
        return switch(type)
        {
            case "boolean" -> "bool";
            case "smallint" -> "int2";
            case "int" -> "int4";
            case "integer" -> "int4";
            case "bigint" -> "int8";
            case "decimal" -> "numeric";
            case "real" -> "float4";
            case "double precision" -> "float8";
            case "character" -> "char";
            case "character varying" -> "varchar";
            default -> type;
        };
    }
}
