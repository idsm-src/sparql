package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;



public class MapUserIriClass extends SimpleUserIriClass
{
    private final String sqlQuery;

    private final Table table;
    private final TableColumn from;
    private final TableColumn to;

    private final Pattern pattern;
    private final String regexp;
    private final String prefix;
    private final String suffix;
    private final int length;


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.table = table;
        this.from = from;
        this.to = to;

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        String code;

        if(prefix == null && suffix == null)
            code = "?::varchar";
        else if(length > 0)
            code = String.format("substring(?, %d, %d)::varchar", prefix != null ? prefix.length() + 1 : 1, length);
        else if(prefix == null)
            code = String.format("left(?, -%d)::varchar", suffix.length());
        else if(suffix == null)
            code = String.format("right(?, -%d)::varchar", prefix.length());
        else
            code = String.format("left(right(?, -%d), -%d)::varchar", prefix.length(), suffix.length());

        this.sqlQuery = String.format("(SELECT %s::varchar FROM %s WHERE %s = %s)", from, table, to, code);


        StringBuilder builder = new StringBuilder();

        builder.append("^(");

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(" + pattern + ")");
        else if(length > 0)
            builder.append(".{").append(length).append("}");
        else
            builder.append(".*");

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        builder.append(")$");

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern, String suffix)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, suffix);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern)
    {
        this(name, sqlType, table, from, to, prefix, length, pattern, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length)
    {
        this(name, sqlType, table, from, to, prefix, length, null, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix)
    {
        this(name, sqlType, table, from, to, prefix, 0, null, null);
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        if(!pattern.matcher(iri.getValue()).matches())
            return false;

        try
        {
            String sql = sqlQuery.replaceAll("\\?", string(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                return result.next();
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        assert match(statement, iri);

        try
        {
            String sql = sqlQuery.replaceAll("\\?", string(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                if(result.next())
                    return List.of(constant(result.getString(1), sqlTypes.get(0)));
                else
                    throw new RuntimeException();
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    protected Column generateFunction(Column column)
    {
        String access = String.format("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = addPrefixAndSuffix(prefix, expression("\"@to\""), suffix);

        return expression("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, column);
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        Column func = generateNonCheckedInverseFunction(column);

        if(!check)
            return func;

        return expression("CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp), func);
    }


    protected Column generateNonCheckedInverseFunction(Column column)
    {
        Column access = expression("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = generateExtractionFunction(column);

        return expression("(SELECT \"@from\"::%s FROM %s WHERE \"@to\" = %s)", sqlTypes.get(0), access, code);
    }


    protected Column generateExtractionFunction(Column column)
    {
        if(prefix == null && suffix == null)
            return column;
        else if(length > 0 && prefix == null)
            return expression("substring(%s, %d, %d)::varchar", column, 1, length);
        else if(length > 0 && prefix != null)
            return expression("substring(%s, %d, %d)::varchar", column, prefix.length() + 1, length);
        else if(prefix == null)
            return expression("left(%s, -%d)::varchar", column, suffix.length());
        else if(suffix == null)
            return expression("right(%s, -%d)::varchar", column, prefix.length());
        else
            return expression("left(right(%s, -%d), -%d)::varchar", column, prefix.length(), suffix.length());
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        Column access = expression("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = suffix != null ? code = expression("\"@to\" || %s", string(suffix)) : expression("\"@to\"");

        return List.of(expression("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, columns.get(0)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 1;
    }


    public Table getTable()
    {
        return table;
    }


    public TableColumn getFrom()
    {
        return from;
    }


    public TableColumn getTo()
    {
        return to;
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

        MapUserIriClass other = (MapUserIriClass) object;

        return Objects.equals(table, other.table) && Objects.equals(from, other.from) && Objects.equals(to, other.to)
                && Objects.equals(regexp, other.regexp) && Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix) && Objects.equals(length, other.length);
    }
}
