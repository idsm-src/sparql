package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
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
 * The variables a FILTER sees. A filter restricts the solutions of its whole group graph pattern, so its expression,
 * the pattern of an EXISTS in it included, sees the variables bound by all members of the group, wherever the filter
 * stands among them, but no variable bound only outside the group or hidden in a MINUS, in an EXISTS or by the
 * projection of a sub-select. A BIND, on the contrary, sees only the members preceding it. The queries select all
 * variables or leave the variables of the filters out, because a projected variable is known before the pattern.
 */
public class FilterScopeTest
{
    private static final String prefix = "PREFIX : <http://example.org/> ";
    private static final Iri g1 = iri("g1");
    private static final Iri g2 = iri("g2");
    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");

    private static SparqlDatabaseConfiguration config = null;
    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), false);

        quad(null, a, p, 1);
        quad(null, a, q, 1);
        quad(null, a, q, 2);
        quad(null, b, p, 3);
        quad(null, b, q, 4);
        quad(null, b, q, 5);
        quad(null, c, p, 2);
        quad(g1, a, p, 1);
        quad(g1, b, p, 3);
        quad(g1, b, q, 4);
        quad(g2, a, p, 1);
        quad(g2, a, q, 1);

        engine = new Engine(config);
    }


    /**
     * Adds a constant quad with an xsd:integer object to the configuration.
     *
     * @param graph the graph, null for the default graph
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     */
    private static void quad(Iri graph, Iri subject, Iri predicate, int object)
    {
        TermMapping mapping = config.createLiteralMapping(xsdInteger, integer(object));

        config.addQuadMapping(graph == null ? null : new ConstantIriMapping(graph), new ConstantIriMapping(subject),
                new ConstantIriMapping(predicate), mapping);
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
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
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static Iri iri(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    /**
     * The xsd:integer literal.
     *
     * @param value the value
     * @return the xsd:integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The xsd:boolean literal.
     *
     * @param value the value
     * @return the xsd:boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("a filter sees a variable bound by a member following it")
    void leadingFilter() throws Exception
    {
        assertThat(execute("SELECT * WHERE { FILTER(?n > 1) ?x :p ?n }"),
                containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
        assertThat(execute("SELECT ?x WHERE { FILTER(?n > 1) ?x :p ?n }"), containsInAnyOrder(row(b), row(c)));
        assertThat(execute("SELECT (COUNT(*) AS ?count) WHERE { FILTER(?n > 1) ?x :p ?n }"), contains(row(integer(2))));
        assertThat(execute("ASK { FILTER(?n > 1) ?x :p ?n }"), contains(row(bool(true))));
        assertThat(execute("ASK { FILTER(?n > 5) ?x :p ?n }"), contains(row(bool(false))));
    }


    @Test
    @DisplayName("the place of a filter among the members of its group does not matter")
    void filterPlace() throws Exception
    {
        for(String pattern : List.of("FILTER(?m > ?n) FILTER(?m != 4) ?x :p ?n . ?x :q ?m",
                "FILTER(?m > ?n) ?x :p ?n FILTER(?m != 4) ?x :q ?m",
                "?x :p ?n FILTER(?m != 4) FILTER(?m > ?n) ?x :q ?m",
                "?x :p ?n . ?x :q ?m FILTER(?m > ?n) FILTER(?m != 4)"))
            assertThat(pattern, execute("SELECT * WHERE { " + pattern + " }"),
                    containsInAnyOrder(row(a, integer(1), integer(2)), row(b, integer(3), integer(5))));
    }


    @Test
    @DisplayName("a filter sees a variable bound by a following pattern of any kind")
    void followingPatterns() throws Exception
    {
        assertThat(execute("SELECT * WHERE { FILTER(?n > 1) { ?x :p ?n } }"),
                containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER(?n > 1) OPTIONAL { ?x :p ?n } }"),
                containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER(?n > 3) { ?x :p ?n } UNION { ?x :q ?n } }"),
                containsInAnyOrder(row(b, integer(4)), row(b, integer(5))));
        assertThat(execute("SELECT * WHERE { FILTER(?m > 3) ?x :p ?n BIND(?n + 1 AS ?m) }"),
                contains(row(b, integer(3), integer(4))));
        assertThat(execute("SELECT * WHERE { FILTER(?n > 1) VALUES ?n { 1 2 } }"), contains(row(integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER(?g = :g1) GRAPH ?g { ?x :q ?m } }"),
                contains(row(g1, b, integer(4))));
        assertThat(execute("SELECT * WHERE { FILTER(?n > 1) { SELECT ?x ?n WHERE { ?x :p ?n } } }"),
                containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER(?count = 3) { SELECT (COUNT(*) AS ?count) WHERE { ?x :p ?n } } }"),
                contains(row(integer(3))));
    }


    @Test
    @DisplayName("the pattern of a leading EXISTS is evaluated for the solutions of its group")
    void leadingExists() throws Exception
    {
        assertThat(execute("SELECT * WHERE { FILTER EXISTS { ?x :q ?m } ?x :p ?n }"),
                containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
        assertThat(execute("SELECT * WHERE { FILTER NOT EXISTS { ?x :q ?m } ?x :p ?n }"), contains(row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER EXISTS { FILTER(?m = ?n) ?x :q ?m } ?x :p ?n }"),
                contains(row(a, integer(1))));
        assertThat(execute("SELECT * WHERE { FILTER NOT EXISTS { FILTER NOT EXISTS { ?x :q ?m } } ?x :p ?n }"),
                containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
        assertThat(execute("SELECT * WHERE { FILTER EXISTS { GRAPH ?g { FILTER(?m = ?n) ?x :q ?m } } ?x :p ?n }"),
                contains(row(a, integer(1))));

        // the current solution is seen in a sub-select of the pattern as well
        assertThat(execute("SELECT * WHERE { FILTER EXISTS { { SELECT ?x WHERE { FILTER(?m > ?n + 1) ?x :q ?m } } } "
                + "?x :p ?n }"), contains(row(b, integer(3))));
    }


    @Test
    @DisplayName("a leading filter of a nested pattern sees the variables of its group")
    void nestedPatterns() throws Exception
    {
        assertThat(execute("SELECT * WHERE { ?x :p ?n OPTIONAL { FILTER(?m > ?n + 1) ?x :q ?m } }"),
                containsInAnyOrder(row(a, integer(1), null), row(b, integer(3), integer(5)), row(c, integer(2), null)));
        assertThat(execute("SELECT * WHERE { ?x :p ?n MINUS { FILTER(?m > 4) ?x :q ?m } }"),
                containsInAnyOrder(row(a, integer(1)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { { FILTER(?n = 1) ?x :p ?n } UNION { FILTER(?n = 2) ?x :p ?n } }"),
                containsInAnyOrder(row(a, integer(1)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { GRAPH ?g { FILTER(?n > 1) ?x :p ?n } }"),
                contains(row(g1, b, integer(3))));
        assertThat(execute("SELECT * WHERE { { SELECT ?x WHERE { FILTER(?n > 1) ?x :p ?n } } }"),
                containsInAnyOrder(row(b), row(c)));
    }


    @Test
    @DisplayName("a filter does not see a variable bound only outside its group")
    void outerVariables() throws Exception
    {
        assertThat(execute("SELECT * WHERE { { FILTER(!BOUND(?n)) } ?x :p ?n }"),
                containsInAnyOrder(row(a, integer(1)), row(b, integer(3)), row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { { FILTER(BOUND(?n)) } ?x :p ?n }"), empty());
        assertThat(execute("SELECT * WHERE { ?x :p ?n { FILTER(!BOUND(?n)) } }"),
                containsInAnyOrder(row(a, integer(1)), row(b, integer(3)), row(c, integer(2))));

        // the right side of the OPTIONAL is joined with what precedes it only
        assertThat(execute("SELECT * WHERE { OPTIONAL { FILTER(?m > ?n) ?x :q ?m } ?x :p ?n }"),
                containsInAnyOrder(row(a, null, integer(1)), row(b, null, integer(3)), row(c, null, integer(2))));
    }


    @Test
    @DisplayName("a filter does not see a variable bound only in a MINUS, in an EXISTS or under a projection")
    void hiddenVariables() throws Exception
    {
        assertThat(execute("SELECT * WHERE { FILTER(BOUND(?m)) ?x :p ?n MINUS { ?x :q ?m } }"), empty());
        assertThat(execute("SELECT * WHERE { FILTER(!BOUND(?m)) ?x :p ?n MINUS { ?x :q ?m } }"),
                contains(row(c, integer(2))));
        assertThat(execute("SELECT * WHERE { FILTER(!BOUND(?m)) ?x :p ?n FILTER EXISTS { ?x :q ?m } }"),
                containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
        assertThat(execute("SELECT * WHERE { FILTER(!BOUND(?n)) { SELECT ?x WHERE { ?x :p ?n } } }"),
                containsInAnyOrder(row(a), row(b), row(c)));
    }


    @Test
    @DisplayName("a BIND sees only the members preceding it")
    void bindOrder() throws Exception
    {
        assertThat(execute("SELECT * WHERE { BIND(BOUND(?n) AS ?bound) ?x :p ?n }"), containsInAnyOrder(
                row(bool(false), a, integer(1)), row(bool(false), b, integer(3)), row(bool(false), c, integer(2))));
        assertThat(execute("SELECT * WHERE { ?x :p ?n BIND(BOUND(?n) AS ?bound) }"), containsInAnyOrder(
                row(a, integer(1), bool(true)), row(b, integer(3), bool(true)), row(c, integer(2), bool(true))));

        // the filter sees the variable of the BIND, whose EXISTS is evaluated for the empty solution
        assertThat(execute("SELECT * WHERE { FILTER(?found) BIND(EXISTS { ?x :q ?n } AS ?found) ?x :p ?n }"),
                containsInAnyOrder(row(bool(true), a, integer(1)), row(bool(true), b, integer(3)),
                        row(bool(true), c, integer(2))));
    }


    @Test
    @DisplayName("the messages of a query keep the order of its text")
    void messageOrder() throws Exception
    {
        String query = "SELECT * WHERE { FILTER(?x = a:x) ?x b:p ?y FILTER EXISTS { FILTER(?z = c:z) ?y d:q ?z } "
                + "{ FILTER(?y = e:y) } FILTER(?y = f:y) ?y g:r ?w }";

        try(Request request = engine.getRequest())
        {
            assertEquals(
                    Stream.of("a", "b", "c", "d", "e", "f", "g")
                            .map(name -> "Could not resolve the prefix: '" + name + "'").toList(),
                    request.check(query).stream().map(TranslateMessage::getMessage).toList());
        }
    }
}
