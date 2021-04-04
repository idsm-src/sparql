package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.engine.Request;



public class IntegerUserIriClass extends SimpleUserIriClass
{
    private final String pattern;
    private final String prefix;
    private final String suffix;
    private final int length;


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length, String suffix)
    {
        super(name, sqlType);

        StringBuffer buffer = new StringBuffer();

        buffer.append(Pattern.quote(prefix));

        if(length > 0)
            buffer.append(String.format("[0-9]{%d}", length));
        else if(sqlType.equals("smallint"))
            buffer.append(generateMaxNumberPattern("32767", -length));
        else if(sqlType.equals("integer"))
            buffer.append(generateMaxNumberPattern("2147483647", -length));
        else if(sqlType.equals("bigint"))
            buffer.append(generateMaxNumberPattern("9223372036854775807", -length));
        else
            throw new IllegalArgumentException("unsupported sql numeric type: " + sqlType);

        if(suffix != null)
            buffer.append(Pattern.quote(suffix));

        this.pattern = buffer.toString();
        this.prefix = prefix;
        this.length = length;
        this.suffix = suffix;
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, String suffix)
    {
        this(name, sqlType, prefix, 0, suffix);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length)
    {
        this(name, sqlType, prefix, length, null);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix)
    {
        this(name, sqlType, prefix, 0, null);
    }


    @Override
    public boolean match(String iri, Request request)
    {
        return iri.matches(pattern);
    }


    @Override
    public boolean match(String iri, DataSource connectionPool)
    {
        return iri.matches(pattern);
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    protected String generateFunction(String parameter)
    {
        String code = String.format("(%s)::varchar", parameter);

        if(length > 0)
            code = String.format("lpad(%s, %d, '0')", code, length);
        else if(length < 0)
            code = String.format("(case when 1%0" + (-1 - length) + "d <= (%s) THEN %s else lpad(%s, %d, '0') end)", 0,
                    parameter, code, code, -length);

        code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        return "(" + code + ")";
    }


    @Override
    protected String generateInverseFunction(String parameter)
    {
        String sqlType = sqlTypes.get(0);

        if(length > 0)
            return String.format("substring(%s, %d, %d)::%s", parameter, prefix.length() + 1, length, sqlType);
        else if(suffix == null)
            return String.format("right(%s, -%d)::%s", parameter, prefix.length(), sqlType);
        else
            return String.format("left(right(%s, -%d), -%d)::%s", parameter, prefix.length(), suffix.length(), sqlType);
    }


    private static String generateMaxNumberPattern(String max, int minLength)
    {
        if(max.length() - minLength < 1)
            throw new IllegalArgumentException("minimal length is to hight");

        StringBuffer buffer = new StringBuffer();

        if(minLength == 0)
            buffer.append(String.format("(0|[1-9][0-9]{0,%d}|", max.length() - 2));
        else if(max.length() - minLength == 1)
            buffer.append(String.format("([0-9]{%d}|", minLength));
        else if(max.length() - minLength == 2)
            buffer.append(String.format("([1-9]?[0-9]{%d}|", minLength));
        else
            buffer.append(String.format("(([1-9][0-9]{0,%d})?[0-9]{%d}|", max.length() - 2 - minLength, minLength));

        buffer.append(String.format("[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, max.length() - 1));

        for(int i = 1; i < max.length() - 1; i++)
            if(max.charAt(i) > '0')
                buffer.append(String.format("%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1,
                        max.length() - i - 1));

        buffer.append(
                String.format("%s[0-%d])", max.substring(0, max.length() - 1), max.charAt(max.length() - 1) - '0'));

        return buffer.toString();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        IntegerUserIriClass other = (IntegerUserIriClass) object;

        if(!pattern.equals(other.pattern))
            return false;

        if(!prefix.equals(other.prefix))
            return false;

        if(suffix == null ? other.suffix != null : !suffix.equals(other.suffix))
            return false;

        if(length != other.length)
            return false;

        return true;
    }
}
