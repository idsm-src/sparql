package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.UserDatatypes.intDatatype;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
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
 * user literal class. A numeric declared class takes the value of every numeric XPath substitutes or promotes to its
 * datatype, promoted, and of no other: an integer, a decimal or a float for a double, a subtype of integer for an
 * integer, but no decimal or double for an integer, no double for a float and no integer for a long. A literal of
 * another kind is not accepted.
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
     * class, the function {@code ex:twice} doubles its double argument, {@code ex:half} halves its float argument,
     * {@code ex:longTwice} doubles its long argument and {@code ex:userTwice} doubles a literal of a user datatype.
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
                    create function extension_argument_value_test.half(value float4) returns float4 \
                    language sql strict as $$ select value / 2 $$""");
            statement.execute("""
                    create function extension_argument_value_test.long_twice(value int8) returns int8 \
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

        config.addFunction(new FunctionDefinition("http://example.org/half",
                new Function("extension_argument_value_test", "half"), xsdFloat, List.of(xsdFloat), true, true));

        config.addFunction(new FunctionDefinition("http://example.org/longTwice",
                new Function("extension_argument_value_test", "long_twice"), xsdLong, List.of(xsdLong), true, true));

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


    /**
     * Decimal literal in the given lexical form.
     */
    private static TypedLiteral decimal(String value)
    {
        return new TypedLiteral(value, xsdDecimalIri);
    }


    /**
     * Float literal in the given lexical form.
     */
    private static TypedLiteral real(String value)
    {
        return new TypedLiteral(value, xsdFloatIri);
    }


    /**
     * Long literal in the given lexical form.
     */
    private static TypedLiteral longInteger(String value)
    {
        return new TypedLiteral(value, xsdLongIri);
    }


    /**
     * Boolean literal.
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    /**
     * Row of an unbound variable.
     */
    private static List<RdfTerm> unbound()
    {
        return row((RdfTerm) null);
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
    @DisplayName("a procedure does not take a literal of another kind")
    void otherKind() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:start '1.5' ] }"), empty());
        assertThat(execute("SELECT ?x WHERE { VALUES ?s { true } ?x ex:series [ ex:start ?s ] }"), empty());
    }


    @Test
    @DisplayName("a procedure takes the promoted value of a numeric constant of another datatype")
    void promotedConstant() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:start 1 ; ex:count 2 ] }"),
                containsInAnyOrder(row(number("1.0E0")), row(number("2.0E0"))));
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:start 1.5 ] }"),
                containsInAnyOrder(row(number("1.5E0"))));
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:start '2.5'^^xsd:float ] }"),
                containsInAnyOrder(row(number("2.5E0"))));
    }


    @Test
    @DisplayName("a procedure takes the promoted value of a variable bound to numerics of several datatypes")
    void promotedVariable() throws Exception
    {
        String query = "SELECT ?s ?x WHERE { VALUES ?s { 1 1.5 '2.5'^^xsd:float '3.5E0'^^xsd:double } "
                + "?x ex:series [ ex:start ?s ] }";

        assertThat(execute(query),
                containsInAnyOrder(row(integer("1"), number("1.0E0")), row(decimal("1.5"), number("1.5E0")),
                        row(real("2.5"), number("2.5E0")), row(number("3.5E0"), number("3.5E0"))));
    }


    @Test
    @DisplayName("a procedure takes the value of a subtype of the datatype of the parameter")
    void substitutedConstant() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:count '2'^^xsd:long ] }"),
                containsInAnyOrder(row(number("5.0E-1")), row(number("1.5E0"))));
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:count '2'^^xsd:unsignedByte ] }"),
                containsInAnyOrder(row(number("5.0E-1")), row(number("1.5E0"))));
    }


    @Test
    @DisplayName("a numeric is not demoted to the datatype of the parameter")
    void noDemotion() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:count 2.0 ] }"), empty());
        assertThat(execute("SELECT ?x WHERE { ?x ex:series [ ex:count '2'^^xsd:double ] }"), empty());
        assertThat(execute("SELECT ?y WHERE { BIND(ex:half('1.0E0'^^xsd:double) AS ?y) }"),
                containsInAnyOrder(unbound()));
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


    @Test
    @DisplayName("a function takes the promoted value of a numeric argument of another datatype")
    void promotedFunctionArgument() throws Exception
    {
        String query = "SELECT ?s ?y WHERE { VALUES ?s { 1 0.5 '0.25'^^xsd:float '2.5E-1'^^xsd:double } "
                + "BIND(ex:half(?s) AS ?y) }";

        assertThat(execute(query), containsInAnyOrder(row(integer("1"), real("5.0E-1")),
                row(decimal("0.5"), real("2.5E-1")), row(real("0.25"), real("1.25E-1")), row(number("2.5E-1"), null)));
        assertThat(execute("SELECT ?y WHERE { BIND(ex:half(1) AS ?y) }"), containsInAnyOrder(row(real("5.0E-1"))));
    }


    @Test
    @DisplayName("a function of a derived integer datatype takes the value of its subtypes only")
    void derivedIntegerArgument() throws Exception
    {
        assertThat(execute("SELECT ?y WHERE { BIND(ex:longTwice('2'^^xsd:int) AS ?y) }"),
                containsInAnyOrder(row(longInteger("4"))));
        assertThat(execute("SELECT ?y WHERE { BIND(ex:longTwice(2) AS ?y) }"), containsInAnyOrder(unbound()));
        assertThat(execute("SELECT ?y WHERE { BIND(ex:longTwice('2'^^xsd:unsignedInt) AS ?y) }"),
                containsInAnyOrder(unbound()));
    }


    @Test
    @DisplayName("a function takes the value of a boxed argument by the datatype of the box")
    void boxedArgument() throws Exception
    {
        // the branches of different classes make the value of IF a box
        String argument = "IF(?c, '1'^^xsd:unsignedInt, '2'^^xsd:int)";

        String pattern = "VALUES ?c { true false } ";

        assertThat(execute("SELECT ?c ?y WHERE { " + pattern + "BIND(ex:longTwice(" + argument + ") AS ?y) }"),
                containsInAnyOrder(row(bool(true), null), row(bool(false), longInteger("4"))));
        assertThat(execute("SELECT ?c ?y WHERE { " + pattern + "BIND(ex:twice(" + argument + ") AS ?y) }"),
                containsInAnyOrder(row(bool(true), number("2.0E0")), row(bool(false), number("4.0E0"))));
    }
}
