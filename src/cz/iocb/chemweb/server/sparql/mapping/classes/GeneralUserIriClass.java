package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.database.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.engine.IriCache;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class GeneralUserIriClass extends UserIriClass
{
    public static enum SqlCheck
    {
        NEVER, IF_MATCH, IF_NOT_MATCH
    }


    private final SqlCheck sqlCheck;
    private final String sqlQuery;
    private final String pattern;
    private final Function function;
    private final List<Function> inverseFunction;


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String pattern,
            SqlCheck sqlCheck)
    {
        super(name, sqlTypes, asList(ResultTag.IRI));

        this.sqlCheck = sqlCheck;
        this.pattern = pattern;
        this.function = new Function(schema, function);

        inverseFunction = new ArrayList<Function>(sqlTypes.size());

        if(sqlTypes.size() == 1)
        {
            inverseFunction.add(new Function(schema, function + "_inverse"));
        }
        else
        {
            for(int i = 0; i < sqlTypes.size(); i++)
                inverseFunction.add(new Function(schema, function + "_inv" + (i + 1)));
        }

        sqlQuery = "SELECT " + inverseFunction.stream().map(f -> f + "(?)").collect(joining(", "));
    }


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String pattern)
    {
        this(name, schema, function, sqlTypes, pattern, SqlCheck.NEVER);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = ((IRI) node);
        assert match(iri);

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit != null)
            return hit;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            for(int i = 1; i <= getColumnCount(); i++)
                statement.setString(i, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                result.next();

                List<Column> columns = new ArrayList<Column>();

                for(int i = 0; i < getColumnCount(); i++)
                {
                    String code = "'" + result.getString(i + 1).replaceAll("'", "''") + "'::" + sqlTypes.get(i);
                    columns.add(new ConstantColumn(code));
                }

                cache.storeToCache(iri, this, columns);
                return columns;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        for(int part = 0; part < getColumnCount(); part++)
        {
            StringBuilder builder = new StringBuilder();

            builder.append(inverseFunction.get(part));
            builder.append("(");
            builder.append(columns.get(0));
            builder.append(")");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        String iri = column.toString();

        for(int part = 0; part < getColumnCount(); part++)
            result.add(new ExpressionColumn(inverseFunction.get(part) + "(" + iri + ")"));

        return result;
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        String iri = "sparql.rdfbox_get_iri(" + column + ")";

        for(int part = 0; part < getColumnCount(); part++)
            result.add(new ExpressionColumn(inverseFunction.get(part) + "(" + iri + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("sparql.rdfbox_create_from_iri(");

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");
        builder.append(")");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match((IRI) node);
    }


    @Override
    public boolean match(IRI iri)
    {
        if(iri.getValue().matches(pattern))
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


    @Override
    public int getCheckCost()
    {
        switch(sqlCheck)
        {
            case IF_MATCH:
                return 1;

            case IF_NOT_MATCH:
                return 2;

            case NEVER:
                return 0;
        }

        return 0;
    }


    private boolean check(IRI iri)
    {
        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit == IriCache.mismatch)
            return false;
        else if(hit != null)
            return true;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            for(int i = 1; i <= getColumnCount(); i++)
                statement.setString(i, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                result.next();

                boolean match = true;

                for(int i = 1; i <= getColumnCount(); i++)
                    if(result.getString(i) == null)
                        match = false;

                if(!match)
                {
                    cache.storeToCache(iri, this, IriCache.mismatch);
                }
                else
                {
                    List<Column> columns = new ArrayList<Column>();

                    for(int i = 0; i < getColumnCount(); i++)
                    {
                        String code = "'" + result.getString(i + 1).replaceAll("'", "''") + "'::" + sqlTypes.get(i);
                        columns.add(new ConstantColumn(code));
                    }

                    cache.storeToCache(iri, this, columns);
                }

                return match;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        GeneralUserIriClass other = (GeneralUserIriClass) object;

        if(!sqlCheck.equals(other.sqlCheck))
            return false;

        if(!pattern.equals(other.pattern))
            return false;

        if(!function.equals(other.function))
            return false;

        if(!inverseFunction.equals(other.inverseFunction))
            return false;

        return true;
    }
}
