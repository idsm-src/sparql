package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * SUM, AVG and GROUP_CONCAT over values that are errors for them, run against an empty configuration. A value that is
 * unbound, no number for SUM and AVG or no string for GROUP_CONCAT makes the aggregate of its group an error, so the
 * aggregate is unbound and what is built on it sees that (BOUND is false and COALESCE takes its next argument); this
 * holds as well for an argument that is such a value in every solution. The aggregate of no solution is still zero or
 * the empty string, whatever its argument is.
 */
public class AggregateErrorTest
{
    private static final TypedLiteral zero = new TypedLiteral("0", xsdIntegerIri);
    private static final TypedLiteral one = new TypedLiteral("1", xsdIntegerIri);
    private static final TypedLiteral two = new TypedLiteral("2", xsdIntegerIri);
    private static final TypedLiteral three = new TypedLiteral("3", xsdIntegerIri);
    private static final TypedLiteral average = new TypedLiteral("1.5", xsdDecimalIri);
    private static final TypedLiteral empty = new TypedLiteral("", xsdStringIri);
    private static final TypedLiteral joined = new TypedLiteral("a b", xsdStringIri);
    private static final TypedLiteral unbound = new TypedLiteral("false", xsdBooleanIri);
    private static final TypedLiteral fallback = new TypedLiteral("fallback", xsdStringIri);

    /**
     * Selection of the aggregates of the numbers {@code ?x} and of the strings {@code ?y}.
     */
    private static final String aggregates = "SELECT (SUM(?x) AS ?s) (AVG(?x) AS ?a) (GROUP_CONCAT(?y) AS ?g) ";

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query run against the empty configuration.
     *
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    @Test
    @DisplayName("the aggregates of numbers and of strings")
    void values() throws Exception
    {
        assertThat(execute(aggregates + "WHERE { VALUES (?x ?y) { (1 \"a\") (2 \"b\") } }"),
                contains(row(three, average, joined)));
    }


    @Test
    @DisplayName("an unbound value makes the aggregate unbound")
    void unboundValue() throws Exception
    {
        String values = "WHERE { VALUES (?x ?y) { (1 \"a\") (UNDEF UNDEF) } }";

        assertThat(execute(aggregates + values), contains(row(null, null, null)));

        for(String aggregate : List.of("SUM(?x)", "AVG(?x)", "GROUP_CONCAT(?y)"))
        {
            String subquery = "{ SELECT (" + aggregate + " AS ?v) " + values + " }";

            assertThat(aggregate, execute(
                    "SELECT ?v (BOUND(?v) AS ?b) (COALESCE(?v, \"fallback\") AS ?c) WHERE { " + subquery + " }"),
                    contains(row(null, unbound, fallback)));
            assertThat(aggregate, execute("SELECT (BOUND(?v) AS ?b) WHERE { " + subquery + " }"),
                    contains(row(unbound)));
        }
    }


    @Test
    @DisplayName("a value of a wrong kind makes the aggregate unbound")
    void wrongValue() throws Exception
    {
        assertThat(execute(aggregates + "WHERE { VALUES (?x ?y) { (1 \"a\") (\"b\" 2) } }"),
                contains(row(null, null, null)));
    }


    @Test
    @DisplayName("an argument of a wrong kind is an error for solutions, but not for no solution")
    void wrongArgument() throws Exception
    {
        assertThat(execute(aggregates + "WHERE { VALUES (?x ?y) { (\"a\" 1) (\"b\" 2) } }"),
                contains(row(null, null, null)));
        assertThat(execute(aggregates + "WHERE { VALUES (?x ?y) { (\"a\" 1) (\"b\" 2) } FILTER(false) }"),
                contains(row(zero, zero, empty)));
    }


    @Test
    @DisplayName("an argument that is never bound is an error for solutions, but not for no solution")
    void unboundArgument() throws Exception
    {
        assertThat(execute(aggregates + "WHERE { VALUES ?z { 1 2 } }"), contains(row(null, null, null)));
        assertThat(execute(aggregates + "WHERE { VALUES ?z { 1 2 } FILTER(false) }"), contains(row(zero, zero, empty)));

        // every group has a solution
        assertThat(
                execute("SELECT ?z (SUM(?x) AS ?s) (GROUP_CONCAT(?y) AS ?g) WHERE { VALUES ?z { 1 2 } } GROUP BY ?z"),
                containsInAnyOrder(row(one, null, null), row(two, null, null)));
    }
}
