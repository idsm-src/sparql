package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static java.lang.String.format;
import java.math.BigInteger;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs of the form {@code prefix + number + suffix} with an integer id stored in an {@code int2}, {@code int4} or
 * {@code int8} column. A positive {@code length} means a zero-padded fixed width, a negative one a zero-padded minimal
 * width, zero no padding.
 */
public class IntegerUserIriClass extends SimpleUserIriClass
{
    /**
     * Compiled regular expression of the IRIs.
     */
    private final Pattern pattern;

    /**
     * Regular expression of the IRIs.
     */
    private final String regexp;

    /**
     * Text before the number.
     */
    private final String prefix;

    /**
     * Text after the number, or null.
     */
    private final String suffix;

    /**
     * Width of the number: positive fixed, negative minimal, zero unpadded.
     */
    private final int length;


    /**
     * Creates the class; {@code pattern} constrains the digits explicitly, otherwise the range of the SQL type is used.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param prefix the prefix
     * @param length width of the number: positive fixed, negative minimal, zero unpadded
     * @param pattern regular expression constraining the digits, or null
     * @param suffix the suffix
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix, int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        StringBuilder builder = new StringBuilder();

        builder.append("^(");
        builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(" + pattern + ")");
        else if(length > 0)
            builder.append(format("[0-9]{%d}", length));
        else if(sqlType.equals(INT2))
            builder.append(generateMaxNumberPattern("32767", -length));
        else if(sqlType.equals(INT4))
            builder.append(generateMaxNumberPattern("2147483647", -length));
        else if(sqlType.equals(INT8))
            builder.append(generateMaxNumberPattern("9223372036854775807", -length));
        else
            throw new IllegalArgumentException("unsupported sql numeric type: " + sqlType);

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        builder.append(")$");

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
    }


    /**
     * Creates the class without an explicit digit pattern.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param prefix the prefix
     * @param length width of the number: positive fixed, negative minimal, zero unpadded
     * @param suffix the suffix
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix, int length, String suffix)
    {
        this(name, sqlType, prefix, length, null, suffix);
    }


    /**
     * Creates the class with unpadded numbers.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param prefix the prefix
     * @param suffix the suffix
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix, String suffix)
    {
        this(name, sqlType, prefix, 0, null, suffix);
    }


    /**
     * Creates the class without a suffix.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param prefix the prefix
     * @param length width of the number: positive fixed, negative minimal, zero unpadded
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix, int length)
    {
        this(name, sqlType, prefix, length, null, null);
    }


    /**
     * Creates the class with unpadded numbers and no suffix.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param prefix the prefix
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix)
    {
        this(name, sqlType, prefix, 0, null, null);
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        assert match(statement, iri);

        String value = iri.getValue();

        String id = value.substring(prefix.length(), value.length() - (suffix != null ? suffix.length() : 0));

        id = id.replaceFirst("^0+", "");

        if(id.isEmpty())
            id = "0";

        return List.of(constant(id, sqlTypes.get(0)));
    }


    @Override
    protected Column generateFunction(Column column)
    {
        return addPrefixAndSuffix(prefix, numberAsString(column), suffix);
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        Column func = generateNonCheckedInverseFunction(column);

        if(!check)
            return func;

        return expression("CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp), func);
    }


    /**
     * SQL expression rendering the number with the configured padding.
     *
     * @param column the column
     * @return SQL expression rendering the number with the configured padding
     */
    private Column numberAsString(Column column)
    {
        if(length == 0)
            return expression("(%s)::varchar", column);
        if(length > 0)
            return expression("lpad((%s)::varchar, %d, '0')", column, length);
        else
            return expression("CASE WHEN %d <= %s THEN (%s)::varchar ELSE lpad((%s)::varchar, %d, '0') END",
                    BigInteger.TEN.pow(-1 - length), column, column, column, -length);
    }


    /**
     * SQL expression extracting the number from an IRI assumed to belong to the class.
     *
     * @param column the column
     * @return SQL expression extracting the number from an IRI assumed to belong to the class
     */
    protected Column generateNonCheckedInverseFunction(Column column)
    {
        SqlType sqlType = sqlTypes.get(0);

        if(length > 0)
            return expression("substring(%s, %d, %d)::%s", column, prefix.length() + 1, length, sqlType);
        else if(suffix == null)
            return expression("right(%s, -%d)::%s", column, prefix.length(), sqlType);
        else
            return expression("left(right(%s, -%d), -%d)::%s", column, prefix.length(), suffix.length(), sqlType);
    }


    /**
     * Regular expression of the decimal numbers from 0 to {@code max} having at least {@code minLength} digits.
     *
     * @param max decimal digits of the largest number
     * @param minLength minimal number of digits
     * @return regular expression of the decimal numbers from 0 to {@code max} having at least {@code minLength} digits
     */
    private static String generateMaxNumberPattern(String max, int minLength)
    {
        if(max.length() - minLength < 1)
            throw new IllegalArgumentException("minimal length is to hight");

        StringBuilder builder = new StringBuilder();

        if(minLength == 0)
            builder.append(format("(0|[1-9][0-9]{0,%d}|", max.length() - 2));
        else if(max.length() - minLength == 1)
            builder.append(format("([0-9]{%d}|", minLength));
        else if(max.length() - minLength == 2)
            builder.append(format("([1-9]?[0-9]{%d}|", minLength));
        else
            builder.append(format("(([1-9][0-9]{0,%d})?[0-9]{%d}|", max.length() - 2 - minLength, minLength));

        builder.append(format("[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, max.length() - 1));

        for(int i = 1; i < max.length() - 1; i++)
            if(max.charAt(i) > '0')
                builder.append(format("%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1,
                        max.length() - i - 1));

        builder.append(format("%s[0-%d])", max.substring(0, max.length() - 1), max.charAt(max.length() - 1) - '0'));

        return builder.toString();
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        if(length > 0)
            return columns;

        return List.of(addPrefixAndSuffix(null, generateNonCheckedInverseFunction(columns.get(0)), suffix));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    /**
     * Text before the number.
     *
     * @return text before the number
     */
    public String getPrefix()
    {
        return prefix;
    }


    /**
     * Text after the number, or null.
     *
     * @return text after the number, or null
     */
    public String getSuffix()
    {
        return suffix;
    }


    /**
     * Width of the number: positive fixed, negative minimal, zero unpadded.
     *
     * @return width of the number: positive fixed, negative minimal, zero unpadded
     */
    public int getIdLength()
    {
        return length;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        IntegerUserIriClass other = (IntegerUserIriClass) object;

        return Objects.equals(regexp, other.regexp) && Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix) && Objects.equals(length, other.length);
    }
}
