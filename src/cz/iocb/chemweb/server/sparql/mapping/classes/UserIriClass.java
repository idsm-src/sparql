package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.db.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UserIriClass extends IriClass
{
    public static enum SqlCheck
    {
        NEVER, IF_MATCH, IF_NOT_MATCH
    }


    private static final int cacheSize = 10000;
    private final DataSource connectionPool;
    private final SqlCheck sqlCheck;
    private final String sqlCheckQuery;
    private final Map<String, Boolean> sqlCheckCache;
    private final String pattern;
    private final String function;
    private final List<String> inverseFunction;


    public UserIriClass(String name, List<String> sqlTypes, String pattern, DataSource pool, SqlCheck sqlCheck)
    {
        super(name, sqlTypes, Arrays.asList(ResultTag.IRI));
        this.connectionPool = pool;
        this.sqlCheck = sqlCheck;
        this.pattern = pattern;
        this.function = name;


        inverseFunction = new ArrayList<String>(sqlTypes.size());

        if(sqlTypes.size() == 1)
        {
            inverseFunction.add(name + "_inverse");
        }
        else
        {
            for(int i = 0; i < sqlTypes.size(); i++)
                inverseFunction.add(name + "_inv" + (i + 1));
        }


        if(sqlCheck != SqlCheck.NEVER)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("select ");

            for(int i = 0; i < sqlTypes.size(); i++)
            {
                if(i > 0)
                    builder.append(" and ");

                builder.append(inverseFunction.get(i));
                builder.append("(?) is not null");
            }

            sqlCheckQuery = builder.toString();


            sqlCheckCache = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true)
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
                {
                    return size() > cacheSize;
                }
            });
        }
        else
        {
            sqlCheckQuery = null;
            sqlCheckCache = null;
        }
    }


    public UserIriClass(String name, List<String> sqlTypes, String pattern)
    {
        this(name, sqlTypes, pattern, null, SqlCheck.NEVER);
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), part);

        IRI iri = (IRI) node;

        return inverseFunction.get(part) + "('" + iri.getUri().toString() + "'::varchar)";
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPatternPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            if(table != null)
                strBuf.append(table).append(".");

            strBuf.append(getSqlColumn(var, i));
        }

        strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(inverseFunction.get(part));
        builder.append("(");

        if(table != null)
            builder.append(table).append(".");

        builder.append(iri.getSqlColumn(var, 0));
        builder.append(")");

        return builder.toString();
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed)
            return inverseFunction.get(part) + "(sparql.rdfbox_extract_iri(" + column + "))";

        return inverseFunction.get(part) + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        StringBuffer strBuf = new StringBuffer();

        if(rdfbox)
            strBuf.append("sparql.cast_as_rdfbox_from_iri(");

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPatternPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(variableAccessor.getSqlVariableAccess(variable, this, i));
        }

        strBuf.append(")");

        if(rdfbox)
            strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public String getResultCode(String var, int part)
    {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPatternPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(getSqlColumn(var, i));
        }

        strBuf.append(")");

        return strBuf.toString();
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match(((IRI) node).getUri().toString());
    }


    public boolean match(String iri)
    {
        if(iri.matches(pattern))
        {
            if(sqlCheck == SqlCheck.IF_MATCH)
                return check(iri);

            return true;
        }
        else
        {
            if(sqlCheck == SqlCheck.IF_NOT_MATCH)
                return check(iri);

            return false;
        }
    }


    private boolean check(String iri)
    {
        Boolean check = sqlCheckCache.get(iri);

        if(check != null)
            return check;

        try(Connection connection = connectionPool.getConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(sqlCheckQuery))
            {
                for(int i = 1; i <= getPatternPartsCount(); i++)
                    statement.setString(i, iri);

                try(ResultSet result = statement.executeQuery())
                {
                    result.next();
                    sqlCheckCache.put(iri, result.getBoolean(1));

                    return result.getBoolean(1);
                }
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public String getIriValueCode(List<String> columns)
    {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(function);
        strBuf.append("(");

        for(int i = 0; i < getPatternPartsCount(); i++)
        {
            if(i > 0)
                strBuf.append(", ");

            strBuf.append(columns.get(i));
        }

        strBuf.append(")");

        return strBuf.toString();
    }
}
