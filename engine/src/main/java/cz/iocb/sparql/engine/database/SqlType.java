package cz.iocb.sparql.engine.database;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;



/**
 * SQL type of a column that holds a part of an RDF term representation. Types are compared by their canonical
 * PostgreSQL name ({@code int4}, not {@code integer}), so that generated casts and constants are spelled uniformly and
 * two columns of the same type are recognised as such regardless of how the configuration named the type. The built-in
 * types are singletons; any other name denotes a {@link UserType}.
 */
public class SqlType
{
    /**
     * Built-in types by their canonical names and accepted aliases.
     */
    private static final Map<String, SqlType> builtins = new HashMap<>();

    /**
     * PostgreSQL {@code boolean}.
     */
    public static final SqlType BOOL = builtin("bool", Boolean.class, "boolean");

    /**
     * PostgreSQL {@code character}.
     */
    public static final SqlType CHAR = builtin("char", Character.class, "character");

    /**
     * PostgreSQL {@code smallint}.
     */
    public static final SqlType INT2 = builtin("int2", Short.class, "smallint");

    /**
     * PostgreSQL {@code integer}.
     */
    public static final SqlType INT4 = builtin("int4", Integer.class, "int", "integer");

    /**
     * PostgreSQL {@code bigint}.
     */
    public static final SqlType INT8 = builtin("int8", Long.class, "bigint");

    /**
     * PostgreSQL {@code numeric}.
     */
    public static final SqlType NUMERIC = builtin("numeric", BigDecimal.class, "decimal");

    /**
     * PostgreSQL {@code real}.
     */
    public static final SqlType FLOAT4 = builtin("float4", Float.class, "real");

    /**
     * PostgreSQL {@code double precision}.
     */
    public static final SqlType FLOAT8 = builtin("float8", Double.class, "double precision");

    /**
     * PostgreSQL {@code character varying}.
     */
    public static final SqlType VARCHAR = builtin("varchar", String.class, "character varying");

    /**
     * PostgreSQL {@code date}.
     */
    public static final SqlType DATE = builtin("date", LocalDate.class);

    /**
     * PostgreSQL {@code timestamp with time zone}.
     */
    public static final SqlType TIMESTAMPTZ = builtin("timestamptz", LocalDateTime.class, "timestamp with time zone");

    /**
     * The universal RDF term type {@code sparql.rdfbox} of the pgsparql extension.
     */
    public static final SqlType RDFBOX = builtin("sparql.rdfbox", null);

    /**
     * The boxed user literal value type {@code sparql.ubox} of the pgsparql extension.
     */
    public static final SqlType UBOX = builtin("sparql.ubox", null);

    /**
     * The {@code sparql.zoneddate} type of the pgsparql extension.
     */
    public static final SqlType ZONEDDATE = builtin("sparql.zoneddate", null);

    /**
     * The {@code sparql.zoneddatetime} type of the pgsparql extension.
     */
    public static final SqlType ZONEDDATETIME = builtin("sparql.zoneddatetime", null);


    /**
     * Canonical name of the type as written in generated SQL.
     */
    private final String name;

    /**
     * Java class the JDBC driver reads values of the type as, or null if values of the type are never read directly.
     */
    private final Class<?> javaClass;


    /**
     * Creates the type.
     *
     * @param name canonical name of the type as written in generated SQL
     * @param javaClass Java class the JDBC driver reads values of the type as, or null
     */
    protected SqlType(String name, Class<?> javaClass)
    {
        this.name = name;
        this.javaClass = javaClass;
    }


    /**
     * Registers a built-in type under its canonical name and aliases.
     *
     * @param name canonical name of the type
     * @param javaClass Java class the JDBC driver reads values of the type as, or null
     * @param aliases other accepted spellings of the type name
     * @return the type
     */
    private static SqlType builtin(String name, Class<?> javaClass, String... aliases)
    {
        SqlType type = new SqlType(name, javaClass);

        builtins.put(name, type);

        for(String alias : aliases)
            builtins.put(alias, type);

        return type;
    }


    /**
     * The type of the given name: the built-in type when the name (or one of its aliases) denotes one, a
     * {@link UserType} otherwise.
     *
     * @param name the type name
     * @return the type
     */
    public static SqlType of(String name)
    {
        SqlType type = builtins.get(name);

        return type != null ? type : new UserType(name);
    }


    /**
     * Canonical name of the type name: the built-in spelling when the name is an alias, the name itself otherwise.
     *
     * @param name the type name
     * @return canonical name of the type name
     */
    static String canonicalName(String name)
    {
        SqlType type = builtins.get(name);

        return type != null ? type.name : name;
    }


    /**
     * Java class the JDBC driver reads values of the built-in type of the given name as, or null.
     *
     * @param name the type name
     * @return Java class the JDBC driver reads values of the built-in type of the given name as, or null
     */
    static Class<?> builtinJavaClass(String name)
    {
        SqlType type = builtins.get(name);

        return type != null ? type.javaClass : null;
    }


    /**
     * Canonical name of the type as written in generated SQL.
     *
     * @return canonical name of the type as written in generated SQL
     */
    public final String getName()
    {
        return name;
    }


    /**
     * Java class the JDBC driver reads values of the type as, or null if values of the type are never read directly.
     *
     * @return Java class the JDBC driver reads values of the type as, or null
     */
    public final Class<?> getJavaClass()
    {
        return javaClass;
    }


    /**
     * True if the type is not one of the built-in types.
     *
     * @return true if the type is not one of the built-in types, false otherwise
     */
    public final boolean isUserType()
    {
        return builtins.get(name) != this;
    }


    /**
     * SQL condition that two not-null values of the type are identical, i.e. represent the same RDF term part. The
     * default is the {@code =} operator of the type; a type whose {@code =} is coarser than the identity (or not
     * hashable or mergeable) may use another operator. The conditions are parenthesised, so that they can be combined
     * with any operator.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two not-null values of the type are identical
     */
    public String equal(Column left, Column right)
    {
        return "(" + left + " = " + right + ")";
    }


    /**
     * SQL condition that two values of the type are identical or both NULL, the null-safe variant of
     * {@link #equal(Column, Column)}.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two values of the type are identical or both NULL
     */
    public String notDistinct(Column left, Column right)
    {
        return "(" + left + " IS NOT DISTINCT FROM " + right + ")";
    }


    @Override
    public final String toString()
    {
        return name;
    }


    @Override
    public final int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public final boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlType other))
            return false;

        return name.equals(other.name);
    }
}
