package cz.iocb.sparql.engine.database;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
     * PostgreSQL {@code character}, which the catalog names {@code bpchar}.
     */
    public static final SqlType CHAR = builtin("char", Character.class, "character", "bpchar");

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

    //FIXME: the equality of the floating-point types takes -0.0 and 0.0 for the same value, whereas they are different
    // terms; the conditions below tell them apart, but DISTINCT and GROUP BY over such columns still merge them

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
     * PostgreSQL {@code text}.
     */
    public static final SqlType TEXT = builtin("text", String.class);

    /**
     * PostgreSQL {@code date}.
     */
    public static final SqlType DATE = builtin("date", LocalDate.class);

    /**
     * PostgreSQL {@code timestamp with time zone}.
     */
    public static final SqlType TIMESTAMPTZ = builtin("timestamptz", OffsetDateTime.class, "timestamp with time zone");

    /**
     * The universal RDF term type {@code sparql.rdfbox} of the pgsparql extension, read from a result as its text form.
     */
    public static final SqlType RDFBOX = builtin("sparql.rdfbox", String.class);

    /**
     * The boxed user literal value type {@code sparql.ubox} of the pgsparql extension, read from a result as its text
     * form ({@code lexical:sqltype}).
     */
    public static final SqlType UBOX = builtin("sparql.ubox", String.class);

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
     * True if the type is one of the types of the pgsparql extension ({@code sparql.rdfbox}, {@code sparql.ubox},
     * {@code sparql.zoneddate} and {@code sparql.zoneddatetime}), whose {@code =} is the value equality and whose
     * operator classes are built on the identity operators {@code @=} and {@code @<>} of the schema {@code sparql}.
     *
     * @return true if the type is one of the types of the pgsparql extension, false otherwise
     */
    private boolean hasIdentityOperators()
    {
        return equals(RDFBOX) || equals(UBOX) || equals(ZONEDDATE) || equals(ZONEDDATETIME);
    }


    /**
     * True if the type is one of the floating-point types {@code float4} and {@code float8}, whose {@code =} takes the
     * zeros of the two signs for equal, although {@code 0.0E0} and {@code -0.0E0} are different RDF terms. (It takes
     * every two NaNs for equal too, which is what the identity needs, as all of them are the term {@code NaN}.)
     *
     * @return true if the type is one of the floating-point types, false otherwise
     */
    private boolean isFloatingPoint()
    {
        return equals(FLOAT4) || equals(FLOAT8);
    }


    /**
     * The binary image of a value of a floating-point type, by which the zeros of the two signs differ.
     *
     * @param value the value
     * @return SQL expression of the binary image of the value
     */
    private SqlCode image(Column value)
    {
        return SqlCode.of(equals(FLOAT4) ? "float4send(" : "float8send(", value, ")");
    }


    /**
     * The binary image of the zero of the other sign than the zero the constant of a floating-point type is, or null
     * when the value is no constant zero.
     *
     * @param value the value
     * @return SQL constant of the binary image of the zero of the other sign, or null when the value is no constant
     *         zero
     */
    private String getOtherZero(Column value)
    {
        if(!(value instanceof ValueColumn constant))
            return null;

        double number;

        try
        {
            number = Double.parseDouble(constant.getValue());
        }
        catch(NumberFormatException e)
        {
            return null;
        }

        if(number != 0)
            return null;

        boolean negative = Double.doubleToRawLongBits(number) != 0;

        return "'\\x" + (negative ? "00" : "80") + (equals(FLOAT4) ? "000000" : "00000000000000") + "'::bytea";
    }


    /**
     * True if the value of a floating-point type is a constant other than a zero (the NULL among them): the {@code =}
     * of the type takes only the values identical to such a constant for equal to it.
     *
     * @param value the value
     * @return true if the value is a constant other than a zero, false otherwise
     */
    private boolean isNonZeroConstant(Column value)
    {
        return value instanceof ConstantColumn && getOtherZero(value) == null;
    }


    /**
     * SQL condition that two not-null values of the type are identical, i.e. represent the same RDF term part. The
     * default is the {@code =} operator of the type; for the types of the pgsparql extension, whose {@code =} is the
     * value equality (two date-times in different zones may be equal), it is the operator {@code @=} of their operator
     * classes, which is the identity and is therefore usable in merge and hash joins. Two values of a floating-point
     * type have to be equal and, when they are zeros, of the same sign: their binary images are compared then, which
     * the condition asks for only among the zeros, so that the database still hashes and looks up by the plain equality
     * and estimates the condition as that equality; a constant zero is compared with the image of the zero of the other
     * sign. The conditions are parenthesised, so that they can be combined with any operator.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two not-null values of the type are identical
     */
    public SqlCode equal(Column left, Column right)
    {
        if(hasIdentityOperators())
            return SqlCode.of("(", left, " operator(sparql.@=) ", right, ")");

        if(!isFloatingPoint() || isNonZeroConstant(left) || isNonZeroConstant(right))
            return SqlCode.of("(", left, " = ", right, ")");

        if(getOtherZero(right) != null && !(left instanceof ConstantColumn))
            return SqlCode.of("(", left, " = ", right, " AND ", image(left), " <> ", getOtherZero(right), ")");

        if(getOtherZero(left) != null && !(right instanceof ConstantColumn))
            return SqlCode.of("(", left, " = ", right, " AND ", image(right), " <> ", getOtherZero(left), ")");

        return SqlCode.of("(", left, " = ", right, " AND (", left, " <> '0'::", name, " OR ", image(left), " = ",
                image(right), "))");
    }


    /**
     * SQL condition that two nullable values of the type are identical or both NULL, the null-safe variant of
     * {@link #equal(Column, Column)}.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two nullable values of the type are identical or both NULL
     */
    public SqlCode notDistinct(Column left, Column right)
    {
        if(hasIdentityOperators())
            return SqlCode.of("COALESCE(", left, " operator(sparql.@=) ", right, ", ", left, " IS NULL AND ", right,
                    " IS NULL)");

        if(!isFloatingPoint() || isNonZeroConstant(left) || isNonZeroConstant(right))
            return SqlCode.of("(", left, " IS NOT DISTINCT FROM ", right, ")");

        if(getOtherZero(right) != null && !(left instanceof ConstantColumn))
            return SqlCode.of("(", left, " IS NOT DISTINCT FROM ", right, " AND ", image(left), " <> ",
                    getOtherZero(right), ")");

        if(getOtherZero(left) != null && !(right instanceof ConstantColumn))
            return SqlCode.of("(", left, " IS NOT DISTINCT FROM ", right, " AND ", image(right), " <> ",
                    getOtherZero(left), ")");

        return SqlCode.of("(", left, " IS NOT DISTINCT FROM ", right, " AND (", left, " <> '0'::", name, " OR ",
                image(left), " IS NOT DISTINCT FROM ", image(right), "))");
    }


    /**
     * SQL condition that two nullable values of the type differ or exactly one of them is NULL, the negation of
     * {@link #notDistinct(Column, Column)}.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two nullable values of the type differ or exactly one of them is NULL
     */
    public SqlCode distinct(Column left, Column right)
    {
        if(hasIdentityOperators())
            return SqlCode.of("(NOT COALESCE(", left, " operator(sparql.@=) ", right, ", ", left, " IS NULL AND ",
                    right, " IS NULL))");

        if(!isFloatingPoint() || isNonZeroConstant(left) || isNonZeroConstant(right))
            return SqlCode.of("(", left, " IS DISTINCT FROM ", right, ")");

        return SqlCode.of("(NOT ", notDistinct(left, right), ")");
    }


    /**
     * SQL condition that two not-null values of the type are not identical, the negation of
     * {@link #equal(Column, Column)}.
     *
     * @param left the left value
     * @param right the right value
     * @return SQL condition that two not-null values of the type are not identical
     */
    public SqlCode notEqual(Column left, Column right)
    {
        if(hasIdentityOperators())
            return SqlCode.of("(", left, " operator(sparql.@<>) ", right, ")");

        if(!isFloatingPoint() || isNonZeroConstant(left) || isNonZeroConstant(right))
            return SqlCode.of("(", left, " != ", right, ")");

        if(getOtherZero(right) != null && !(left instanceof ConstantColumn))
            return SqlCode.of("(", left, " != ", right, " OR ", image(left), " = ", getOtherZero(right), ")");

        if(getOtherZero(left) != null && !(right instanceof ConstantColumn))
            return SqlCode.of("(", left, " != ", right, " OR ", image(right), " = ", getOtherZero(left), ")");

        return SqlCode.of("(", left, " != ", right, " OR (", left, " = '0'::", name, " AND ", image(left), " != ",
                image(right), "))");
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
