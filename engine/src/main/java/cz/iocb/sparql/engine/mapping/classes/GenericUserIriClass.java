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
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs recognised by a regular expression and converted by SQL functions supplied by the deployment: {@code function}
 * builds the IRI from the columns, {@code function_inverse} (or {@code function_inv1} .. {@code function_invN} for
 * several columns) extract them. {@link SqlCheck} says when a term must additionally be verified through the inverse
 * functions in the database.
 */
public class GenericUserIriClass extends UserIriClass
{
    /**
     * When an IRI must be verified in the database: never, only when the regular expression matches, or only when it
     * does not.
     */
    public static enum SqlCheck
    {
        /**
         * Trust the regular expression alone.
         */
        NEVER,

        /**
         * Verify IRIs that match the regular expression.
         */
        IF_MATCH,

        /**
         * Verify IRIs that do not match the regular expression.
         */
        IF_NOT_MATCH
    }


    /**
     * When to verify IRIs in the database.
     */
    protected final SqlCheck sqlCheck;

    /**
     * Query applying the inverse functions to a placeholder IRI.
     */
    protected final String sqlQuery;

    /**
     * Compiled regular expression.
     */
    protected final Pattern pattern;

    /**
     * Regular expression recognising the IRIs.
     */
    protected final String regexp;

    /**
     * SQL function building the IRI from the columns.
     */
    protected final Function function;

    /**
     * SQL functions extracting each column from the IRI.
     */
    protected final List<Function> inverseFunction;


    /**
     * Creates the class; the inverse functions are derived from the function name ({@code _inverse}, or {@code _inv1}
     * .. {@code _invN} for several columns).
     *
     * @param name the name
     * @param schema the schema name
     * @param function name of the SQL function building the IRI
     * @param sqlTypes the SQL types
     * @param regexp regular expression recognising the IRIs
     * @param sqlCheck when to verify IRIs in the database
     */
    public GenericUserIriClass(String name, String schema, String function, List<SqlType> sqlTypes, String regexp,
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


    /**
     * Creates the class trusting the regular expression alone.
     *
     * @param name the name
     * @param schema the schema name
     * @param function name of the SQL function building the IRI
     * @param sqlTypes the SQL types
     * @param pattern regular expression recognising the IRIs
     */
    public GenericUserIriClass(String name, String schema, String function, List<SqlType> sqlTypes, String pattern)
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
            String sql = sqlQuery.replace("?", string(iri.getValue()));

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
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
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


    /**
     * True if all inverse functions return non-null for the IRI.
     *
     * @param statement database statement used for lookups in the database
     * @param iri the IRI
     * @return true if all inverse functions return non-null for the IRI, false otherwise
     */
    private boolean check(Statement statement, Iri iri)
    {
        try
        {
            String sql = sqlQuery.replace("?", string(iri.getValue()));

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

        GenericUserIriClass other = (GenericUserIriClass) object;

        return Objects.equals(sqlCheck, other.sqlCheck) && Objects.equals(regexp, other.regexp)
                && Objects.equals(function, other.function) && Objects.equals(inverseFunction, other.inverseFunction);
    }
}
