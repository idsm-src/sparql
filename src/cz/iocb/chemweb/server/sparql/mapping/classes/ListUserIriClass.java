package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.engine.Request;



public class ListUserIriClass extends SimpleUserIriClass
{
    private static final int cacheSize = 10000;
    private final Map<String, Boolean> sqlCheckCache;
    private final String sqlCheckQuery;


    public ListUserIriClass(String name, Table table, TableColumn column)
    {
        super(name, "varchar");

        this.sqlCheckCache = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
            {
                return size() > cacheSize;
            }
        });

        this.sqlCheckQuery = String.format("(select 1 from %s where %s = ?::varchar)", table.getCode(),
                column.getCode());
    }


    @Override
    public boolean match(String iri, Request request)
    {
        return check(iri, request);
    }


    @Override
    public boolean match(String iri, DataSource connectionPool)
    {
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
        return parameter;
    }


    @Override
    protected String generateInverseFunction(String parameter)
    {
        return parameter;
    }
}
