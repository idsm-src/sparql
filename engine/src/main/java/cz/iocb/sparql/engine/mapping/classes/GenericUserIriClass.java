package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.stream.Collectors.joining;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import cz.iocb.sparql.engine.request.Request;



/**
 * IRIs recognised by a regular expression and converted by SQL functions supplied by the deployment: {@code function}
 * builds the IRI from the columns, {@code function_inverse} (or {@code function_inv1} .. {@code function_invN} for
 * several columns) extract them. {@link SqlCheck} says when a term must additionally be verified through the inverse
 * functions in the database. The inverse functions may assume an IRI the regular expression recognises: when a general
 * IRI is converted to the class with the representability check, an IRI the expression does not recognise yields NULL
 * without reaching them (unless the IRIs the expression does not recognise are to be verified by them).
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
     * The regular expression anchored to the whole IRI for the search of the extension.
     */
    protected final String anchoredRegexp;

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
        this.anchoredRegexp = "^(?:" + regexp + ")$";
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
    public boolean match(Request request, Iri iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(matcher.matches())
            return sqlCheck != SqlCheck.IF_MATCH || check(request, iri);
        else
            return sqlCheck == SqlCheck.IF_NOT_MATCH && check(request, iri);
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return "";
    }


    @Override
    public List<Column> toColumns(Request request, Iri iri)
    {
        assert match(request, iri);

        try
        {
            String sql = sqlQuery.replace("?", string(iri.getValue()));

            try(ResultSet result = request.getStatement().executeQuery(sql))
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
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        String call = columns.stream().map(Object::toString).collect(joining(", ", function + "(", ")"));

        if(targetClass.equals(box))
            return List.of(expression(RDFBOX, "sparql.rdfbox_create_from_iri(%s)", call));

        if(targetClass.equals(iri))
            return List.of(expression(VARCHAR, "%s", call));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        // the check is needless when every IRI of the superclass belongs to this class
        boolean check = !checkOptional && !superClass.isSubclassOf(unionize(this, subtract(box, iri)));

        if(sourceClass.equals(box))
            return generateInverseFunctions(expression(VARCHAR, "sparql.rdfbox_get_iri(%s)", columns.get(0)), check);

        if(sourceClass.equals(iri))
            return generateInverseFunctions(columns.get(0), check);

        throw new IllegalArgumentException();
    }


    /**
     * SQL expressions applying the inverse functions to the IRI. With {@code check}, an IRI the regular expression does
     * not recognise yields NULL instead of being given to the functions, which may assume a recognised IRI; an IRI
     * recognised wrongly is for the functions to refuse by NULL, as {@link SqlCheck#IF_MATCH} says. The IRIs that
     * {@link SqlCheck#IF_NOT_MATCH} leaves to the functions reach them in any case.
     *
     * @param iri SQL expression of the IRI
     * @param check whether IRIs outside the class must yield NULL
     * @return SQL expressions applying the inverse functions to the IRI
     */
    private List<Column> generateInverseFunctions(Column iri, boolean check)
    {
        List<Column> result = new ArrayList<>(inverseFunction.size());

        for(int i = 0; i < inverseFunction.size(); i++)
        {
            Column value = expression(sqlTypes.get(i), "%s(%s)", inverseFunction.get(i), iri);

            if(check && sqlCheck != SqlCheck.IF_NOT_MATCH)
                value = expression(sqlTypes.get(i), "CASE WHEN sparql.regex_string(%s, %s) THEN %s END", iri,
                        string(anchoredRegexp), value);

            result.add(value);
        }

        return result;
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
     * @param request the current request
     * @param iri the IRI
     * @return true if all inverse functions return non-null for the IRI, false otherwise
     */
    private boolean check(Request request, Iri iri)
    {
        try
        {
            String sql = sqlQuery.replace("?", string(iri.getValue()));

            try(ResultSet result = request.getStatement().executeQuery(sql))
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
