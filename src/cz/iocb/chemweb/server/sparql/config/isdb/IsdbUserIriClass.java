package cz.iocb.chemweb.server.sparql.config.isdb;

import static java.util.Arrays.asList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.engine.IriCache;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class IsdbUserIriClass extends UserIriClass
{
    private final String pattern;
    private final String prefix;
    private final String suffix;
    private final String sqlQuery;
    private final int prefixLen;
    private final int suffixLen;

    public IsdbUserIriClass(String name, String prefix, String suffix)
    {
        super(name, asList("integer", "char"), asList(ResultTag.IRI));

        //FIXME: check whether the pattern is valid also in pcre2
        this.pattern = Pattern.quote(prefix) + "([A-Z]{14}-[NP])" + (suffix != null ? Pattern.quote(suffix) : "");
        this.prefix = prefix;
        this.suffix = suffix;

        prefixLen = prefix.length();
        suffixLen = suffix != null ? suffix.length() : 0;
        sqlQuery = "select id::varchar from isdb.compound_bases where accession = ?";
    }


    public IsdbUserIriClass(String name, String prefix)
    {
        this(name, prefix, null);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(iri);

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit != null)
            return hit;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            statement.setString(1, iri.getValue().substring(prefixLen, prefixLen + 14));

            try(ResultSet result = statement.executeQuery())
            {
                if(result.next())
                {
                    List<Column> columns = asList(
                            new ConstantColumn("'" + result.getString(1).replaceAll("'", "''") + "'::integer"),
                            new ConstantColumn("'" + iri.getValue().charAt(prefix.length() + 15) + "'::char"));
                    cache.storeToCache(iri, this, columns);
                    return columns;
                }
                else
                {
                    throw new RuntimeException();
                }
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public boolean match(IRI iri)
    {
        if(!iri.getValue().matches(pattern))
            return false;

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit == IriCache.mismatch)
            return false;
        else if(hit != null)
            return true;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            statement.setString(1, iri.getValue().substring(prefixLen, prefixLen + 14));

            try(ResultSet result = statement.executeQuery())
            {
                boolean match = result.next();

                if(!match)
                {
                    cache.storeToCache(iri, this, IriCache.mismatch);
                }
                else
                {
                    List<Column> columns = asList(
                            new ConstantColumn(result.getString(1).replaceAll("'", "''") + "::integer"),
                            new ConstantColumn("'" + iri.getValue().charAt(prefixLen + 15) + "'::char"));
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
    public int getCheckCost()
    {
        return 1;
    }


    protected Column generateFunction(List<Column> columns)
    {
        String access = "(SELECT id as \"@from\", accession as \"@to\" FROM isdb.compound_bases) as \"@rctab\"";

        String code = String.format("'%s' || \"@to\" || '-' || %s", prefix.replaceAll("'", "''"), columns.get(1),
                columns.get(1).toString());

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        code = String.format("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, columns.get(0));

        return new ExpressionColumn(code);
    }



    protected List<Column> generateInverseFunctions(Column parameter, boolean check)
    {
        String access = "(SELECT id as \"@from\", accession as \"@to\" FROM isdb.compound_bases) as \"@rctab\"";
        String col1 = String.format("(SELECT \"@from\"::integer FROM %s WHERE \"@to\" = substring(%s, %d, 14)::varchar",
                access, parameter, prefixLen + 1);
        String col2 = String.format("right(%s, %i)::char", parameter.toString(), suffixLen + 1);

        if(check)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN sparql.regex_string(");
            builder.append(parameter);
            builder.append(", '^(");
            builder.append(pattern.replaceAll("'", "''"));
            builder.append(")$', '') THEN ");

            col1 = builder.toString() + col1 + " END";
            col2 = builder.toString() + col2 + " END";
        }

        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn(col1));
        result.add(new ExpressionColumn(col2));

        return result;
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return generateInverseFunctions(columns.get(0), true);
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return asList(generateFunction(columns));
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        return generateInverseFunctions(column, false);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return generateFunction(columns);
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        return generateInverseFunctions(new ExpressionColumn("sparql.rdfbox_get_iri(" + column + ")"), check);
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_iri(" + generateFunction(columns) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return asList(generateFunction(columns));
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
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        IsdbUserIriClass other = (IsdbUserIriClass) object;

        if(!prefix.equals(other.prefix))
            return false;

        return true;
    }
}
