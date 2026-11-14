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
 * does not need. A branch of IF is then built under the restriction of the parent of the IF, also when the branch is a
 * test or a logical operation, whose boolean value the parent does not need, and when the other branch is held in a
 * box, whose class overlaps the classes the branch is left without.
 */
public class IfCoalesceUnneededClassTest
{
    private static final String values = "VALUES (?c ?x ?y) { (true 2 \"a\") (false \"b\" 1) (true UNDEF 3) "
            + "(false UNDEF \"c\") }";
    private static final String union = "{ { VALUES ?v { \"a\" } } UNION { VALUES ?c { false } } }";
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
    @DisplayName("a test in a branch of IF whose boolean value the parent does not need")
    void testInBranch() throws Exception
    {
        assertThat(execute("SELECT (STRLEN(IF(?c, REGEX(?v, \"a\"), \"zz\")) AS ?x) WHERE " + union),
                containsInAnyOrder(unbound, integer("2")));
        assertThat(execute("SELECT (IF(?c, REGEX(?v, \"a\"), 1) + 1 AS ?x) WHERE " + union),
                containsInAnyOrder(unbound, integer("2")));
        assertThat(execute("SELECT (STRLEN(IF(?c, !REGEX(?v, \"a\"), \"zz\")) AS ?x) WHERE " + union),
                containsInAnyOrder(unbound, integer("2")));
        assertThat(
                execute("SELECT ?w WHERE { { VALUES ?v { \"a\" } } UNION { VALUES ?w { 1 } } "
                        + "FILTER(BOUND(?w) || STRLEN(IF(BOUND(?w), \"a\", REGEX(?v, \"a\"))) > 0) }"),
                containsInAnyOrder(integer("1")));
    }


    @Test
    @DisplayName("a logical operation in a branch of IF whose boolean value the parent does not need")
    void logicalOperationInBranch() throws Exception
    {
        assertThat(execute("SELECT (STRLEN(IF(?c, !?v, \"zz\")) AS ?x) WHERE " + union),
                containsInAnyOrder(unbound, integer("2")));
        assertThat(execute("SELECT (STRLEN(IF(?c, ?v && true, \"zz\")) AS ?x) WHERE " + union),
                containsInAnyOrder(unbound, integer("2")));
    }


    @Test
    @DisplayName("a branch of IF left without a class beside a branch held in a box")
    void branchBesideBox() throws Exception
    {
        assertThat(
                execute("SELECT (STRLEN(IF(?c, ?x, COALESCE(?y, 1))) AS ?l) WHERE { VALUES (?c ?x ?y) "
                        + "{ (true 2 \"a\") (false 3 \"b\") (false 4 UNDEF) } }"),
                containsInAnyOrder(unbound, integer("1"), unbound));
        assertThat(
                execute("SELECT (-IF(?c, ?y, COALESCE(?x, \"z\")) AS ?l) WHERE { VALUES (?c ?x ?y) "
                        + "{ (true 2 \"a\") (false 3 \"b\") (false UNDEF 4) } }"),
                containsInAnyOrder(unbound, integer("-3"), unbound));

        // the binary operation takes the IF in the box, with the class of the other branch left out
        assertThat(
                execute("SELECT (IF(?c, STRDT(\"5\", <http://www.w3.org/2001/XMLSchema#integer>), \"zz\") + 1 AS ?l) "
                        + "WHERE { VALUES ?c { true false } }"),
                containsInAnyOrder(integer("6"), unbound));
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
