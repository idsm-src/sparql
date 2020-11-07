package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.engine.Request;



public class StringUserIriClass extends SimpleUserIriClass
{
    private final String pattern;
    private final String prefix;
    private final String suffix;
    private final int length;


    public StringUserIriClass(String name, String prefix, int length, String pattern, String suffix)
    {
        super(name, "varchar");

        if(pattern == null)
            pattern = length > 0 ? ".{" + length + "}" : ".*";

        StringBuffer buffer = new StringBuffer();

        if(prefix != null)
            buffer.append(Pattern.quote(prefix));

        buffer.append(pattern);

        if(suffix != null)
            buffer.append(Pattern.quote(suffix));

        this.pattern = buffer.toString();
        this.prefix = prefix;
        this.length = length;
        this.suffix = suffix;
    }


    public StringUserIriClass(String name, String prefix, String pattern, String suffix)
    {
        this(name, prefix, 0, pattern, suffix);
    }


    public StringUserIriClass(String name, String prefix, int length, String pattern)
    {
        this(name, prefix, length, pattern, null);
    }


    public StringUserIriClass(String name, String prefix, String pattern)
    {
        this(name, prefix, 0, pattern, null);
    }


    public StringUserIriClass(String name, String prefix, int length)
    {
        this(name, prefix, length, null, null);
    }


    public StringUserIriClass(String name, String prefix)
    {
        this(name, prefix, 0, null, null);
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
        String code = parameter;

        if(prefix == null && suffix == null)
            return code;

        if(prefix != null)
            code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        return "(" + code + ")::varchar";
    }


    @Override
    protected String generateInverseFunction(String parameter)
    {
        if(prefix == null && suffix == null)
            return parameter;
        else if(length > 0)
            return String.format("substring(%s, %d, %d)::varchar", parameter, prefix != null ? prefix.length() + 1 : 1,
                    length);
        else if(prefix == null)
            return String.format("left(%s, -%d)::varchar", parameter, suffix.length());
        else if(suffix == null)
            return String.format("right(%s, -%d)::varchar", parameter, prefix.length());
        else
            return String.format("left(right(%s, -%d), -%d)::varchar", parameter, prefix.length(), suffix.length());
    }
}
