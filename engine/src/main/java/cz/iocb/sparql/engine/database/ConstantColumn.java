package cz.iocb.sparql.engine.database;



public class ConstantColumn extends Column
{
    private final String literal;

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


    public String getValue()
    {
        return literal;
    }


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
