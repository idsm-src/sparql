package cz.iocb.sparql.engine.mapping.classes;

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
import cz.iocb.sparql.engine.rdf.Iri;



public class IntegerUserIriClass extends SimpleUserIriClass
{
    private final Pattern pattern;
    private final String regexp;
    private final String prefix;
    private final String suffix;
    private final int length;


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length, String pattern, String suffix)
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
        else if(sqlType.equals("int2"))
            builder.append(generateMaxNumberPattern("32767", -length));
        else if(sqlType.equals("int4"))
            builder.append(generateMaxNumberPattern("2147483647", -length));
        else if(sqlType.equals("int8"))
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


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length, String suffix)
    {
        this(name, sqlType, prefix, length, null, suffix);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, String suffix)
    {
        this(name, sqlType, prefix, 0, null, suffix);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length)
    {
        this(name, sqlType, prefix, length, null, null);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix)
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


    protected Column generateNonCheckedInverseFunction(Column column)
    {
        String sqlType = sqlTypes.get(0);

        if(length > 0)
            return expression("substring(%s, %d, %d)::%s", column, prefix.length() + 1, length, sqlType);
        else if(suffix == null)
            return expression("right(%s, -%d)::%s", column, prefix.length(), sqlType);
        else
            return expression("left(right(%s, -%d), -%d)::%s", column, prefix.length(), suffix.length(), sqlType);
    }


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


    public String getPrefix()
    {
        return prefix;
    }


    public String getSuffix()
    {
        return suffix;
    }


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
