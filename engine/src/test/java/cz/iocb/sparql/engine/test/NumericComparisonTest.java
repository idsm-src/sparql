package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Numerics of different datatypes are compared in the least datatype every operand is derived from or promoted to (the
 * hierarchy of XML Schema), so that the operands are taken in it as XPath takes the arguments of a function: an
 * unsigned datatype meets a signed one of a bigger size in xsd:integer, not in the SQL type holding both, also when the
 * operand holds values of several datatypes in a box. The class of such an operand spans the datatypes (a value of IF
 * with branches of two datatypes, a sum, which is an integer when there is nothing to sum, an average), so the datatype
 * of the comparison is taken from all of them.
 */
public class NumericComparisonTest
{
    /**
     * A numeric datatype with two of its literals.
     *
     * @param name the local name of the datatype
     * @param low the lexical form of the literal of the lesser value
     * @param high the lexical form of the literal of the greater value
     */
    private record Numeric(String name, String low, String high)
    {
        /**
         * The literal of the datatype as a query writes it.
         *
         * @param form the lexical form of the literal
         * @return the literal as a query writes it
         */
        private String literal(String form)
        {
            return "'" + form + "'^^xsd:" + name;
        }


        @Override
        public String toString()
        {
            return "xsd:" + name;
        }
    }


    /**
     * A comparison operator.
     *
     * @param text the operator as a query writes it
     * @param test the test of the sign of the left operand compared with the right one
     */
    private record Operator(String text, IntPredicate test)
    {
    }


    /**
     * A comparison of the operand of two datatypes with a constant.
     *
     * @param expression the comparison
     * @param first the value of the comparison when the operand is of the first datatype
     * @param second the value of the comparison when the operand is of the second datatype
     */
    private record Comparison(String expression, boolean first, boolean second)
    {
    }


    /**
     * The numeric datatypes, each with a lesser and a greater literal: every datatype holds its values exactly, and the
     * values of different datatypes are less, equal and greater.
     */
    private static final List<Numeric> numerics = List.of(new Numeric("integer", "1", "3"),
            new Numeric("decimal", "1.5", "3.0"), new Numeric("float", "1.5", "3"),
            new Numeric("double", "1.5e0", "3.0e0"), new Numeric("short", "1", "3"), new Numeric("long", "1", "3"),
            new Numeric("unsignedInt", "1", "3"));

    /**
     * The comparison operators.
     */
    private static final List<Operator> operators = List.of(new Operator("=", d -> d == 0),
            new Operator("!=", d -> d != 0), new Operator("<", d -> d < 0), new Operator("<=", d -> d <= 0),
            new Operator(">", d -> d > 0), new Operator(">=", d -> d >= 0));

    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Configuration without mappings.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the configuration.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();
        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
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
     * Literal of the datatype in the given lexical form.
     */
    private static TypedLiteral literal(String value, Iri datatype)
    {
        return new TypedLiteral(value, datatype);
    }


    @Test
    @DisplayName("an unsigned datatype is compared with a signed one by value")
    void mixedDatatypes() throws Exception
    {
        String query = """
                SELECT ?a ?b WHERE { VALUES (?a ?b) { \
                ('200'^^xsd:unsignedByte '100'^^xsd:short) ('-1'^^xsd:short '0'^^xsd:unsignedByte) \
                ('65535'^^xsd:unsignedShort '1'^^xsd:int) ('4294967295'^^xsd:unsignedInt '1'^^xsd:long) \
                ('1'^^xsd:unsignedByte '2'^^xsd:short) } FILTER(?a > ?b) }""";

        assertThat(execute(query),
                containsInAnyOrder(row(literal("200", xsdUnsignedByteIri), literal("100", xsdShortIri)),
                        row(literal("65535", xsdUnsignedShortIri), literal("1", xsdIntIri)),
                        row(literal("4294967295", xsdUnsignedIntIri), literal("1", xsdLongIri))));
    }


    @Test
    @DisplayName("a box holding an unsigned or a signed datatype is compared with a signed one by value")
    void boxedOperand() throws Exception
    {
        // the branches of different classes make the value of IF a box
        String query = "SELECT ?c WHERE { VALUES ?c { true false } "
                + "BIND(IF(?c, '1'^^xsd:unsignedByte, '2'^^xsd:short) AS ?x) FILTER(?x < '3'^^xsd:short) }";

        assertThat(execute(query),
                containsInAnyOrder(row(literal("true", xsdBooleanIri)), row(literal("false", xsdBooleanIri))));
    }


    /**
     * Every pair of the numeric datatypes.
     *
     * @return the pairs of the numeric datatypes
     */
    static Stream<Arguments> getPairs()
    {
        return numerics.stream().flatMap(first -> numerics.stream().map(second -> Arguments.of(first, second)));
    }


    @ParameterizedTest(name = "{0} or {1}")
    @MethodSource("getPairs")
    @DisplayName("an operand holding values of two datatypes is compared by value with every datatype")
    void operandOfTwoDatatypes(Numeric first, Numeric second) throws Exception
    {
        // the branches of different datatypes make the class of the value of IF span both
        String operand = "IF(?c, ?a, ?b)";
        String pattern = "VALUES (?c ?a ?b) { (true %1$s %2$s) (false %1$s %2$s) }"
                .formatted(first.literal(first.low()), second.literal(second.high()));

        for(Numeric third : numerics)
        {
            List<Comparison> comparisons = new ArrayList<>();

            for(String form : List.of(third.low(), third.high()))
            {
                int low = new BigDecimal(first.low()).compareTo(new BigDecimal(form));
                int high = new BigDecimal(second.high()).compareTo(new BigDecimal(form));

                for(Operator operator : operators)
                {
                    comparisons.add(new Comparison(operand + " " + operator.text() + " " + third.literal(form),
                            operator.test().test(low), operator.test().test(high)));
                    comparisons.add(new Comparison(third.literal(form) + " " + operator.text() + " " + operand,
                            operator.test().test(-low), operator.test().test(-high)));
                }
            }

            StringBuilder query = new StringBuilder("SELECT");

            for(int i = 0; i < comparisons.size(); i++)
                query.append(" (").append(comparisons.get(i).expression()).append(" AS ?e").append(i).append(")");

            query.append(" WHERE { ").append(pattern).append(" } ORDER BY DESC(?c)");

            List<List<RdfTerm>> rows = execute(query.toString());

            assertEquals(2, rows.size());

            for(int i = 0; i < comparisons.size(); i++)
            {
                Comparison comparison = comparisons.get(i);

                assertEquals(literal(Boolean.toString(comparison.first()), xsdBooleanIri), rows.get(0).get(i),
                        comparison.expression() + " for a value of " + first);
                assertEquals(literal(Boolean.toString(comparison.second()), xsdBooleanIri), rows.get(1).get(i),
                        comparison.expression() + " for a value of " + second);
            }
        }
    }


    @Test
    @DisplayName("a sum or an average is compared by value with a numeric of another datatype")
    void aggregate() throws Exception
    {
        String decimals = "VALUES (?g ?x) { (1 1.5) (1 2.5) (2 0.5) (3 10.25) (3 1.0) (4 3.0) }";
        String integers = "VALUES (?g ?x) { (1 1) (1 2) (2 5) (3 10) (3 1) (4 3) }";
        String doubles = "VALUES (?g ?x) { (1 1.5e0) (1 2.5e0) (2 0.5e0) (3 10.25e0) (3 1.0e0) (4 3.0e0) }";

        // a sum of decimals is a decimal, or the integer zero when there is nothing to sum
        assertThat(execute("SELECT ?g WHERE { " + decimals + " } GROUP BY ?g HAVING(SUM(?x) > 3)"),
                containsInAnyOrder(row(literal("1", xsdIntegerIri)), row(literal("3", xsdIntegerIri))));
        assertThat(execute("SELECT (SUM(?x) AS ?s) WHERE { " + decimals + " FILTER(?x > 20) } HAVING(SUM(?x) < 1)"),
                containsInAnyOrder(row(literal("0", xsdIntegerIri))));

        // an average of integers is a decimal
        assertThat(execute("SELECT ?g WHERE { " + integers + " } GROUP BY ?g HAVING(AVG(?x) >= 3)"), containsInAnyOrder(
                row(literal("2", xsdIntegerIri)), row(literal("3", xsdIntegerIri)), row(literal("4", xsdIntegerIri))));

        // an average of doubles meets a float in xsd:double
        assertThat(execute("SELECT ?g WHERE { " + doubles + " } GROUP BY ?g HAVING(AVG(?x) > '2.5'^^xsd:float)"),
                containsInAnyOrder(row(literal("3", xsdIntegerIri)), row(literal("4", xsdIntegerIri))));
    }
}
