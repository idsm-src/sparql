package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
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
 * IF and COALESCE whose arguments take classes the parent does not need, run against an empty configuration: the
 * functions are built for the needed classes only, and COALESCE still stops at an argument bound in a class the parent
 * does not need.
 */
public class IfCoalesceUnneededClassTest
{
    private static final String values = "VALUES (?c ?x ?y) { (true 2 \"a\") (false \"b\" 1) (true UNDEF 3) "
            + "(false UNDEF \"c\") }";
    private static final List<RdfTerm> unbound = row((RdfTerm) null);

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


    /**
     * Row of an xsd:integer literal.
     *
     * @param value lexical form of the literal
     * @return row of the literal
     */
    private static List<RdfTerm> integer(String value)
    {
        return row(new TypedLiteral(value, xsdIntegerIri));
    }


    @Test
    @DisplayName("a numeric operation on IF")
    void conditional() throws Exception
    {
        assertThat(execute("SELECT (-IF(?c, ?x, ?y) AS ?m) WHERE { " + values + " }"),
                containsInAnyOrder(integer("-2"), integer("-1"), unbound, unbound));
        assertThat(
                execute("SELECT (-IF(?c, ?x, ?y) AS ?m) WHERE { VALUES (?c ?x ?y) { (true 2 \"a\") "
                        + "(false \"b\" 1.5e0) (true \"c\" 1) } }"),
                containsInAnyOrder(integer("-2"), row(new TypedLiteral("-1.5E0", xsdDoubleIri)), unbound));
    }


    @Test
    @DisplayName("a numeric operation on COALESCE stops at an argument of another class")
    void coalesce() throws Exception
    {
        assertThat(execute("SELECT (-COALESCE(?x, ?y) AS ?m) WHERE { " + values + " }"),
                containsInAnyOrder(integer("-2"), unbound, integer("-3"), unbound));
        assertThat(execute("SELECT (-COALESCE(?x, 5) AS ?m) WHERE { " + values + " }"),
                containsInAnyOrder(integer("-2"), unbound, integer("-5"), integer("-5")));
        assertThat(execute(
                "SELECT ?x WHERE { VALUES (?x ?y) { (2 \"a\") } " + "FILTER(STRSTARTS(COALESCE(?x, ?y), \"a\")) }"),
                containsInAnyOrder());
    }


    @Test
    @DisplayName("COALESCE in a grouping and in a filtered BIND")
    void coalesceElsewhere() throws Exception
    {
        assertThat(execute("SELECT ?g WHERE { " + values + " } GROUP BY (-COALESCE(?x, ?y) AS ?g)"),
                containsInAnyOrder(integer("-2"), integer("-3"), unbound));
        assertThat(execute("SELECT ?m WHERE { " + values + " BIND(-COALESCE(?x, ?y) AS ?m) FILTER(BOUND(?m)) }"),
                containsInAnyOrder(integer("-2"), integer("-3")));
    }
}
