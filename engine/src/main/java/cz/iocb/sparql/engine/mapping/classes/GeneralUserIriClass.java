package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static java.util.stream.Collectors.joining;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.rdf.Iri;



public class GeneralUserIriClass extends UserIriClass
{
    public static enum SqlCheck
    {
        NEVER, IF_MATCH, IF_NOT_MATCH
    }


    protected final SqlCheck sqlCheck;
    protected final String sqlQuery;
    protected final Pattern pattern;
    protected final String regexp;
    protected final Function function;
    protected final List<Function> inverseFunction;


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String regexp,
            SqlCheck sqlCheck)
    {
        super(name, sqlTypes, Set.of(box, iri));

        this.sqlCheck = sqlCheck;
        this.function = new Function(schema, function);

        this.inverseFunction = new ArrayList<>(sqlTypes.size());

        if(sqlTypes.size() == 1)
        {
            this.inverseFunction.add(new Function(schema, function + "_inverse"));
        }
        else
        {
            for(int i = 0; i < sqlTypes.size(); i++)
                this.inverseFunction.add(new Function(schema, function + "_inv" + (i + 1)));
        }

        this.sqlQuery = "SELECT " + inverseFunction.stream().map(f -> f + "(?)").collect(joining(", "));

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = regexp;
        this.pattern = Pattern.compile(regexp);
    }


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String pattern)
    {
        this(name, schema, function, sqlTypes, pattern, SqlCheck.NEVER);
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(matcher.matches())
            return sqlCheck != SqlCheck.IF_MATCH || check(statement, iri);
        else
            return sqlCheck == SqlCheck.IF_NOT_MATCH && check(statement, iri);
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return "";
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
                result.next();

                List<Column> columns = new ArrayList<>();

                for(int i = 0; i < getColumnCount(); i++)
                    columns.add(constant(result.getString(i + 1), sqlTypes.get(i)));

                return columns;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        if(superClass.equals(this))
            return columns;

        String call = columns.stream().map(Object::toString).collect(joining(", ", function + "(", "))"));

        if(superClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_iri(" + call + ")"));

        if(superClass.equals(iri))
            return List.of(expression(call));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return inverseFunction.stream().map(f -> expression(f + "(sparql.rdfbox_get_iri(" + columns.get(0) + "))"))
                    .toList();

        if(sourceClass.equals(iri))
            return inverseFunction.stream().map(f -> expression(f + "(" + columns.get(0) + ")")).toList();

        throw new IllegalArgumentException();
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


    private boolean check(Statement statement, Iri iri)
    {
        try
        {
            String sql = sqlQuery.replaceAll("\\?", string(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                result.next();

                for(int i = 1; i <= getColumnCount(); i++)
                    if(result.getString(i) == null)
                        return false;

                return true;
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

        return Objects.equals(sqlCheck, other.sqlCheck) && Objects.equals(regexp, other.regexp)
                && Objects.equals(function, other.function) && Objects.equals(inverseFunction, other.inverseFunction);
    }
}
