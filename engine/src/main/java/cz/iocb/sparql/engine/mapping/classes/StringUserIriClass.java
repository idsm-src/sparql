package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.parser.model.IRI;



public class StringUserIriClass extends SimpleUserIriClass
{
    private final Pattern pattern;
    private final String regexp;
    private final String prefix;
    private final String suffix;
    private final int length;


    public StringUserIriClass(String name, String prefix, int length, String pattern, String suffix)
    {
        super(name, "varchar");

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        StringBuilder builder = new StringBuilder();

        builder.append("^(");

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        if(pattern == null)
            pattern = length > 0 ? ".{" + length + "}" : ".*";

        builder.append("(" + pattern + ")");

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        builder.append(")$");

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
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
    public boolean match(Statement statement, IRI iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Statement statement, IRI iri)
    {
        assert match(statement, iri);

        String value = iri.getValue();
        String id = value.substring(prefix.length(), value.length() - (suffix != null ? suffix.length() : 0));

        return List.of(constant(id, "varchar"));
    }


    @Override
    protected Column generateFunction(Column column)
    {
        return addPrefixAndSuffix(prefix, column, suffix);
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        Column func = generateNonCheckedInverseFunction(column);

        if(!check)
            return func;

        return expression("CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp), func);
    }


    private Column generateNonCheckedInverseFunction(Column column)
    {
        String sqlType = sqlTypes.get(0);

        if(prefix == null && suffix == null)
            return column;
        else if(length > 0 && prefix == null)
            return expression("substring(%s, %d, %d)::%s", column, 1, length, sqlType);
        else if(length > 0)
            return expression("substring(%s, %d, %d)::%s", column, prefix.length() + 1, length, sqlType);
        else if(prefix == null)
            return expression("left(%s, -%d)::%s", column, suffix.length(), sqlType);
        else if(suffix == null)
            return expression("right(%s, -%d)::%s", column, prefix.length(), sqlType);
        else
            return expression("left(right(%s, -%d), -%d)::%s", column, prefix.length(), suffix.length(), sqlType);
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        return List.of(addPrefixAndSuffix(null, columns.get(0), suffix));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ConstantColumn col)
            return (prefix != null ? prefix : "") + col.getValue() + (suffix != null ? suffix : "");

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

        StringUserIriClass other = (StringUserIriClass) object;

        return Objects.equals(regexp, other.regexp) && Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix) && Objects.equals(length, other.length);
    }
}
