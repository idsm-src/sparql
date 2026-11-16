package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static java.lang.String.format;
import static java.util.Locale.ROOT;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * IRIs of the form {@code prefix + number + suffix} with an integer id stored in an {@code int2}, {@code int4} or
 * {@code int8} column. A positive {@code length} means a zero-padded fixed width, a negative one a zero-padded minimal
 * width, zero no padding. Unless a pattern constrains the digits, the regular expression of the IRIs admits just the
 * numbers from zero to the largest number of the SQL type, so that an IRI of a greater number is no member, whose id
 * the database could not hold.
 */
public class IntegerUserIriClass extends SimpleUserIriClass
{
    /**
     * Compiled regular expression of the IRIs.
     */
    private final Pattern pattern;

    /**
     * Regular expression of the IRIs, anchored to the whole IRI (see {@link #wholeIriRegexp}); Java evaluates it in
     * {@link #match} and PCRE2 in the conversions with the representability check.
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
     * @param pattern regular expression constraining the digits, or null; it alone decides which numbers belong to the
     *            class, so it has to admit only numbers of the SQL type written with the padding of the class, and both
     *            {@code java.util.regex} and PCRE2 (in the UTF mode without UCP) evaluate it, so it has to keep to
     *            their common subset (not, e.g., {@code \p{javaDigit}}, an intersection of character classes, a POSIX
     *            property name like {@code \p{Alpha}} or {@code (?U)})
     * @param suffix the suffix
     */
    public IntegerUserIriClass(String name, SqlType sqlType, String prefix, int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        StringBuilder builder = new StringBuilder();

        builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(?:" + pattern + ")");
        else if(length > 0)
            builder.append(generateFixedWidthPattern(getMaxNumber(sqlType), length));
        else
            builder.append(generateMaxNumberPattern(getMaxNumber(sqlType), -length));

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        this.regexp = wholeIriRegexp(builder.toString());
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
    public boolean match(Request request, Iri iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Request request, Iri iri)
    {
        assert match(request, iri);

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

        return expression(sqlTypes.get(0), "CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp),
                func);
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
            return expression(VARCHAR, "(%s)::varchar", column);
        if(length > 0)
            return expression(VARCHAR, "lpad((%s)::varchar, %d, '0')", column, length);
        else
            return expression(VARCHAR, "CASE WHEN %d <= %s THEN (%s)::varchar ELSE lpad((%s)::varchar, %d, '0') END",
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
            return expression(sqlType, "substring(%s, %d, %d)::%s", column, prefix.length() + 1, length, sqlType);
        else if(suffix == null)
            return expression(sqlType, "right(%s, -%d)::%s", column, prefix.length(), sqlType);
        else
            return expression(sqlType, "left(right(%s, -%d), -%d)::%s", column, prefix.length(), suffix.length(),
                    sqlType);
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
            builder.append(format(ROOT, "(0|[1-9][0-9]{0,%d}|", max.length() - 2));
        else if(max.length() - minLength == 1)
            builder.append(format(ROOT, "([0-9]{%d}|", minLength));
        else if(max.length() - minLength == 2)
            builder.append(format(ROOT, "([1-9]?[0-9]{%d}|", minLength));
        else
            builder.append(format(ROOT, "(([1-9][0-9]{0,%d})?[0-9]{%d}|", max.length() - 2 - minLength, minLength));

        builder.append(generateFullLengthPattern(max));
        builder.append(")");

        return builder.toString();
    }


    /**
     * Regular expression of the decimal numbers from 0 to {@code max} written with exactly {@code width} digits, padded
     * by leading zeros.
     *
     * @param max decimal digits of the largest number
     * @param width number of digits
     * @return regular expression of the decimal numbers from 0 to {@code max} written with exactly {@code width} digits
     */
    private static String generateFixedWidthPattern(String max, int width)
    {
        if(width < max.length())
            return format(ROOT, "[0-9]{%d}", width);

        return format(ROOT, "%s(0[0-9]{%d}|%s)", "0".repeat(width - max.length()), max.length() - 1,
                generateFullLengthPattern(max));
    }


    /**
     * Regular expression of the decimal numbers not greater than {@code max} that have as many digits as it, the first
     * of them not a zero.
     *
     * @param max decimal digits of the largest number, the first of them greater than one
     * @return regular expression of the decimal numbers not greater than {@code max} that have as many digits as it
     */
    private static String generateFullLengthPattern(String max)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(format(ROOT, "[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, max.length() - 1));

        for(int i = 1; i < max.length() - 1; i++)
            if(max.charAt(i) > '0')
                builder.append(format(ROOT, "%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1,
                        max.length() - i - 1));

        builder.append(
                format(ROOT, "%s[0-%d]", max.substring(0, max.length() - 1), max.charAt(max.length() - 1) - '0'));

        return builder.toString();
    }


    /**
     * Decimal digits of the largest number of the SQL type.
     *
     * @param sqlType the SQL type
     * @return decimal digits of the largest number of the SQL type
     * @throws IllegalArgumentException if the type is no integer type
     */
    private static String getMaxNumber(SqlType sqlType)
    {
        if(sqlType.equals(INT2))
            return Short.toString(Short.MAX_VALUE);
        else if(sqlType.equals(INT4))
            return Integer.toString(Integer.MAX_VALUE);
        else if(sqlType.equals(INT8))
            return Long.toString(Long.MAX_VALUE);
        else
            throw new IllegalArgumentException("unsupported sql numeric type: " + sqlType);
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        if(length > 0)
            return columns;

        return List.of(addPrefixAndSuffix(null, numberAsString(columns.get(0)), suffix));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public List<String> getSqlRegexps()
    {
        return List.of(regexp);
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
