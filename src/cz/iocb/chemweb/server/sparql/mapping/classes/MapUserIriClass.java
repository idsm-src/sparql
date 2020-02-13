package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.engine.Request;



public class MapUserIriClass extends SimpleUserIriClass
{
    private static final int cacheSize = 10000;
    private final Map<String, Boolean> sqlCheckCache;
    private final String sqlCheckQuery;

    private final Table table;
    private final TableColumn from;
    private final TableColumn to;

    private final String pattern;
    private final String prefix;
    private final String suffix;
    private final int length;


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern, String suffix)
    {
        super(name, sqlType);

        StringBuffer buffer = new StringBuffer();

        if(prefix != null)
            buffer.append(Pattern.quote(prefix));

        if(pattern != null)
            buffer.append(pattern);
        else if(length > 0)
            buffer.append(".{").append(length).append("}");
        else
            buffer.append(".*");

        if(suffix != null)
            buffer.append(Pattern.quote(suffix));

        this.table = table;
        this.from = from;
        this.to = to;

        this.pattern = buffer.toString();
        this.prefix = prefix;
        this.length = length;
        this.suffix = suffix;

        this.sqlCheckCache = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
            {
                return size() > cacheSize;
            }
        });


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

        this.sqlCheckQuery = String.format("(select 1 from %s where %s = %s)", table.getCode(), to.getCode(), code);
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


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String pattern)
    {
        this(name, sqlType, table, from, to, null, 0, pattern, null);
    }


    @Override
    public boolean match(String iri, Request request)
    {
        if(!iri.matches(pattern))
            return false;

        return check(iri, request);
    }


    @Override
    public boolean match(String iri, DataSource connectionPool)
    {
        if(!iri.matches(pattern))
            return false;

        return check(iri, connectionPool);
    }


    private boolean check(String iri, Request request)
    {
        Boolean check = sqlCheckCache.get(iri);

        if(check != null)
            return check;

        try(PreparedStatement statement = request.getStatement(sqlCheckQuery))
        {
            statement.setString(1, iri);

            try(ResultSet result = statement.executeQuery())
            {
                boolean value = result.next();
                sqlCheckCache.put(iri, value);
                return value;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    private boolean check(String iri, DataSource connectionPool)
    {
        Boolean check = sqlCheckCache.get(iri);

        if(check != null)
            return check;

        try(Connection connection = connectionPool.getConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(sqlCheckQuery))
            {
                statement.setString(1, iri);

                try(ResultSet result = statement.executeQuery())
                {
                    boolean value = result.next();
                    sqlCheckCache.put(iri, value);
                    return value;
                }
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    protected String generateFunction(String parameter)
    {
        String code = to.getCode();

        if(prefix != null)
            code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        return String.format("(select (%s)::varchar from %s where %s = %s)", code, table.getCode(), from.getCode(),
                parameter);
    }


    @Override
    protected String generateInverseFunction(String parameter)
    {
        String code;

        if(prefix == null && suffix == null)
            code = parameter;
        else if(length > 0)
            code = String.format("substring(%s, %d, %d)::varchar", parameter, prefix != null ? prefix.length() + 1 : 1,
                    length);
        else if(prefix == null)
            code = String.format("left(%s, -%d)::varchar", parameter, suffix.length());
        else if(suffix == null)
            code = String.format("right(%s, -%d)::varchar", parameter, prefix.length());
        else
            code = String.format("left(right(%s, -%d), -%d)::varchar", parameter, prefix.length(), suffix.length());

        return String.format("(select (%s)::%s from %s where %s = %s)", from.getCode(), sqlTypes.get(0),
                table.getCode(), to.getCode(), code);
    }
}
