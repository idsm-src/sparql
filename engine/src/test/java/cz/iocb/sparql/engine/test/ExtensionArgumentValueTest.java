package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.UserDatatypes.intDatatype;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.SubsetLiteralClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * A procedure or an extension function takes the values of its parameters in the columns of their declared classes, so
 * a literal of the datatype of a canonical declared class supplies its value in every valid lexical form, not only in
 * the canonical one the class holds: a constant, a default value and a variable bound to a non-canonical literal give
 * the function the value of the literal, and the variable keeps its lexical form. This holds for a class of any column
 * layout (the composite date-time class), for a subset class, whose narrowing does not apply to the values, and for a
 * user literal class. A literal of another datatype is still not accepted.
 */
public class ExtensionArgumentValueTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Integers up to ten, a subset class of {@code xsd:integer}.
     */
    private static final SubsetLiteralClass smallInteger = new SubsetLiteralClass("small-integer", xsdInteger)
    {
        @Override
        public boolean match(Request request, Literal literal)
        {
            return super.match(request, literal) && new BigInteger(literal.getValue()).compareTo(BigInteger.TEN) <= 0;
        }
    };

    /**
     * Configuration mapping the procedures and the functions.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code extension_argument_value_test} schema: the procedure {@code ex:series} returns the doubles
     * {@code ex:start}, {@code ex:start + 1}, ... ({@code ex:count} of them; the start defaults to the non-canonical
     * {@code "0.5"^^xsd:double}, the count to 1), {@code ex:smallSeries} does the same with the count declared in a
     * subset class, {@code ex:year} returns the year of the date-time {@code ex:value}, declared in the composite
     * class, the function {@code ex:twice} doubles its argument and {@code ex:userTwice} doubles a literal of a user
     * datatype.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema extension_argument_value_test");
            statement.execute("""
                    create function extension_argument_value_test.series(start float8, count numeric) \
                    returns setof float8 language sql strict as $$ \
                    select start + i from generate_series(0, count::int - 1) i $$""");
            statement.execute("""
                    create function extension_argument_value_test.year_of(value timestamptz, zone int4) \
                    returns setof numeric language sql strict as $$ \
                    select extract(year from value at time zone 'UTC')::numeric $$""");
            statement.execute("""
                    create function extension_argument_value_test.twice(value float8) returns float8 \
                    language sql strict as $$ select 2 * value $$""");
            statement.execute("""
                    create function extension_argument_value_test.user_twice(value int4) returns numeric \
                    language sql strict as $$ select 2 * value $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addDatatype(intDatatype);

        ProcedureDefinition series = new ProcedureDefinition("http://example.org/series",
                new Function("extension_argument_value_test", "series"));
        series.addParameter(new ParameterDefinition("http://example.org/start", xsdDouble,
                new LiteralNode("0.5", new IriNode(xsdDoubleIri.getValue()))));
        series.addParameter(new ParameterDefinition("http://example.org/count", xsdInteger,
                new LiteralNode("1", new IriNode(xsdIntegerIri.getValue()))));
        series.addResult(new ResultDefinition(xsdDouble));
        config.addProcedure(series);

        ProcedureDefinition smallSeries = new ProcedureDefinition("http://example.org/smallSeries",
                new Function("extension_argument_value_test", "series"));
        smallSeries.addParameter(new ParameterDefinition("http://example.org/start", xsdDouble,
                new LiteralNode("0.5", new IriNode(xsdDoubleIri.getValue()))));
        smallSeries.addParameter(new ParameterDefinition("http://example.org/count", smallInteger, null));
        smallSeries.addResult(new ResultDefinition(xsdDouble));
        config.addProcedure(smallSeries);

        ProcedureDefinition year = new ProcedureDefinition("http://example.org/year",
                new Function("extension_argument_value_test", "year_of"));
        year.addParameter(new ParameterDefinition("http://example.org/value", xsdDateTime, null));
        year.addResult(new ResultDefinition(xsdInteger));
        config.addProcedure(year);

        config.addFunction(new FunctionDefinition("http://example.org/twice",
                new Function("extension_argument_value_test", "twice"), xsdDouble, List.of(xsdDouble), true, true));

        config.addFunction(new FunctionDefinition("http://example.org/userTwice",
                new Function("extension_argument_value_test", "user_twice"), xsdInteger,
                List.of(intDatatype.getCanonicalLiteralClass()), true, true));
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * Double literal in the given lexical form.
     */
    private static TypedLiteral number(String value)
    {
        return new TypedLiteral(value, xsdDoubleIri);
    }


    /**
     * Integer literal in the given lexical form.
     */
    private static TypedLiteral integer(String value)
    {
        return new TypedLiteral(value, xsdIntegerIri);
    }


    @Test
    @DisplayName("a procedure takes the value of a non-canonical constant")
    void nonCanonicalConstant() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:series [ ex:start '1.5'^^xsd:double ; ex:count '+02'^^xsd:integer ] }";

        assertThat(execute(query), containsInAnyOrder(row(number("1.5E0")), row(number("2.5E0"))));
    }


    @Test
    @DisplayName("a procedure takes the value of a non-canonical default")
    void nonCanonicalDefault() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:series [ ex:count 2 ] }";

        assertThat(execute(query), containsInAnyOrder(row(number("5.0E-1")), row(number("1.5E0"))));
    }


    @Test
    @DisplayName("a procedure takes the value of a variable, which keeps its lexical form")
    void variableParameter() throws Exception
    {
        String query = "SELECT ?s ?x WHERE { VALUES ?s { '1.5'^^xsd:double '2.5E0'^^xsd:double } "
                + "?x ex:series [ ex:start ?s ] }";

        assertThat(execute(query),
                containsInAnyOrder(row(number("1.5"), number("1.5E0")), row(number("2.5E0"), number("2.5E0"))));
    }


    @Test
    @DisplayName("a procedure does not take a literal of another datatype")
    void otherDatatype() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:start 1.5 ] }"), empty());
        assertThat(execute("SELECT ?x WHERE { VALUES ?s { 1 } ?x ex:series [ ex:start ?s ] }"), empty());
    }


    @Test
    @DisplayName("a parameter of a composite class takes the value of a non-canonical date-time")
    void compositeDateTimeParameter() throws Exception
    {
        String query = "SELECT ?y WHERE { ?y ex:year [ ex:value '2020-06-15T12:00:00.0Z'^^xsd:dateTime ] }";

        assertThat(execute(query), containsInAnyOrder(row(integer("2020"))));
    }


    @Test
    @DisplayName("a parameter of a subset class takes the value of a non-canonical integer, whatever the subset")
    void subsetParameter() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:smallSeries [ ex:count '+02'^^xsd:integer ] }"),
                containsInAnyOrder(row(number("5.0E-1")), row(number("1.5E0"))));
        assertThat(execute("SELECT ?x WHERE { ?x ex:smallSeries [ ex:count 20 ] }"), hasSize(20));
    }


    @Test
    @DisplayName("a function takes the value of a non-canonical argument")
    void functionArgument() throws Exception
    {
        String query = "SELECT ?s ?y WHERE { VALUES ?s { '0.25'^^xsd:double '2.5E-1'^^xsd:double } "
                + "BIND(ex:twice(?s) AS ?y) }";

        assertThat(execute(query),
                containsInAnyOrder(row(number("0.25"), number("5.0E-1")), row(number("2.5E-1"), number("5.0E-1"))));
        assertThat(execute("SELECT ?y WHERE { BIND(ex:twice('0.25'^^xsd:double) AS ?y) }"),
                containsInAnyOrder(row(number("5.0E-1"))));
    }


    @Test
    @DisplayName("a function takes the value of a non-canonical user literal")
    void userLiteralArgument() throws Exception
    {
        String query = "SELECT ?y WHERE { BIND(ex:userTwice('+05'^^<http://example.org/datatype#int>) AS ?y) }";

        assertThat(execute(query), containsInAnyOrder(row(integer("10"))));
    }
}
