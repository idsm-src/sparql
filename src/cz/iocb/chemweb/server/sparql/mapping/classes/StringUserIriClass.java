package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import java.util.regex.Pattern;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



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

        StringBuilder builder = new StringBuilder();

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        builder.append(pattern);

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        this.pattern = builder.toString(); //FIXME: check whether the pattern is valid also in pcre2
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
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(iri);

        String value = iri.getValue();

        String id = value.substring(prefix.length(), value.length() - (suffix != null ? suffix.length() : 0));

        return asList(new ConstantColumn("'" + id.replaceAll("'", "''") + "'::varchar"));
    }


    @Override
    public boolean match(IRI iri)
    {
        return iri.getValue().matches(pattern);
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    protected Column generateFunction(Column parameter)
    {
        if(prefix == null && suffix == null)
            return parameter;

        String code = parameter.toString();

        if(prefix != null)
            code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        return new ExpressionColumn("(" + code + ")::varchar");
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        if(prefix == null && suffix == null && !check)
            return parameter;

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE WHEN sparql.regex_string(");
            builder.append(parameter);
            builder.append(", '^(");
            builder.append(pattern.replaceAll("'", "''"));
            builder.append(")$', '') THEN ");
        }

        if(prefix == null && suffix == null)
            builder.append(parameter);
        else if(length > 0)
            builder.append(String.format("substring(%s, %d, %d)::", parameter, prefix != null ? prefix.length() + 1 : 1,
                    length));
        else if(prefix == null)
            builder.append(String.format("left(%s, -%d)::", parameter, suffix.length()));
        else if(suffix == null)
            builder.append(String.format("right(%s, -%d)::", parameter, prefix.length()));
        else
            builder.append(String.format("left(right(%s, -%d), -%d)::", parameter, prefix.length(), suffix.length()));

        builder.append(sqlTypes.get(0));

        if(check)
            builder.append(" END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        StringUserIriClass other = (StringUserIriClass) object;

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
}
