package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Scope of the variable of a GRAPH clause: a pattern inside {@code GRAPH ?g} is evaluated separately in every named
 * graph, so a sub-select applies its DISTINCT, ORDER BY, OFFSET, LIMIT and aggregation per graph (an aggregation
 * without GROUP BY has a solution even in a graph without matching triples), the sides of a MINUS are evaluated in the
 * same graph without the graph variable counting as a variable they share, and a pattern whose own triples do not bind
 * the graph (VALUES, BIND, an empty group, a UNION with such a branch) still applies its MINUS, OPTIONAL and EXISTS to
 * the triples of the current graph only.
 */
public class GraphScopeTest
{
    private static final String prefix = "PREFIX : <http://example.org/> ";
    private static final Iri g1 = iri("g1");
    private static final Iri g2 = iri("g2");
    private static final Iri g3 = iri("g3");
    private static final Iri s = iri("s");
    private static final Iri s1 = iri("s1");
    private static final Iri s2 = iri("s2");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");
    private static final Iri o = iri("o");
    private static final Iri o1 = iri("o1");
    private static final Iri o2 = iri("o2");
    private static final Iri o3 = iri("o3");
    private static final Iri o9 = iri("o9");

    private static DataSource connectionPool = null;
    private static DatabaseSchema schema = null;
    private static SparqlDatabaseConfiguration config = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();
        schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        quad(null, s, p, o);
        quad(g1, s, p, o);
        quad(g1, s1, p, o1);
        quad(g1, s1, p, o2);
        quad(g1, s1, p, o3);
        quad(g1, s, q, g2);
        quad(g2, s, p, o);
        quad(g2, s1, p, o1);
        quad(g2, s2, p, o1);
        quad(g2, s2, p, o2);
        quad(g2, s, q, g2);
        quad(g3, s, p, o);
    }


    /**
     * Adds a constant quad to the configuration.
     *
     * @param graph the graph, null for the default graph
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     */
    private static void quad(Iri graph, Iri subject, Iri predicate, Iri object)
    {
        config.addQuadMapping(graph == null ? null : new ConstantIriMapping(graph), new ConstantIriMapping(subject),
                new ConstantIriMapping(predicate), new ConstantIriMapping(object));
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


    @Test
    @DisplayName("ORDER BY, OFFSET and LIMIT of a sub-select apply separately in every graph")
    void slice() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?s ?o WHERE {
                  GRAPH ?g { SELECT ?s ?o { ?s :p ?o } ORDER BY ?s ?o OFFSET 1 LIMIT 2 }
                }"""), containsInAnyOrder(row(g1, s1, o1), row(g1, s1, o2), row(g2, s1, o1), row(g2, s2, o1)));
    }


    @Test
    @DisplayName("an aggregation without GROUP BY has one solution in every graph, including an empty one")
    void aggregation() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?c WHERE {
                  GRAPH ?g { SELECT (COUNT(*) AS ?c) { ?s :p ?o FILTER(?o != :o) } }
                }"""), containsInAnyOrder(row(g1, integer(3)), row(g2, integer(3)), row(g3, integer(0))));
    }


    @Test
    @DisplayName("the groups of a sub-select are formed separately in every graph")
    void grouping() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?s ?c WHERE {
                  GRAPH ?g { SELECT ?s (COUNT(*) AS ?c) { ?s :p ?o } GROUP BY ?s }
                }"""), containsInAnyOrder(row(g1, s, integer(1)), row(g1, s1, integer(3)), row(g2, s, integer(1)),
                row(g2, s1, integer(1)), row(g2, s2, integer(2)), row(g3, s, integer(1))));
    }


    @Test
    @DisplayName("DISTINCT of a sub-select applies separately in every graph")
    void distinct() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?o WHERE {
                  GRAPH ?g { SELECT DISTINCT ?o { ?s :p ?o } }
                }"""), containsInAnyOrder(row(g1, o), row(g1, o1), row(g1, o2), row(g1, o3), row(g2, o), row(g2, o1),
                row(g2, o2), row(g3, o)));
    }


    @Test
    @DisplayName("a sub-select nested in a sub-select is evaluated in the same graph")
    void nested() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?o WHERE {
                  GRAPH ?g {
                    SELECT ?o { { SELECT ?o { ?s :p ?o } ORDER BY ?o LIMIT 2 } } ORDER BY DESC(?o) LIMIT 1
                  }
                }"""), containsInAnyOrder(row(g1, o1), row(g2, o1), row(g3, o)));
    }


    @Test
    @DisplayName("a sub-select without triples is evaluated in every graph")
    void withoutTriples() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?x WHERE {
                  GRAPH ?g { SELECT ?x { VALUES ?x { 2 1 } } ORDER BY ?x LIMIT 1 }
                }"""), containsInAnyOrder(row(g1, integer(1)), row(g2, integer(1)), row(g3, integer(1))));
    }


    @Test
    @DisplayName("the graph variable projected by a sub-select is a variable of its own, joined with the graph")
    void projectedGraphVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?s WHERE {
                  GRAPH ?g { SELECT ?g ?s { ?s :q ?g } }
                }"""), containsInAnyOrder(row(g2, s)));
    }


    @Test
    @DisplayName("the graph variable is not a variable shared by the sides of a MINUS")
    void minusWithoutSharedVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?a WHERE {
                  GRAPH ?g { ?a :p :o MINUS { ?b :p :o } }
                }"""), containsInAnyOrder(row(g1, s), row(g2, s), row(g3, s)));
    }


    @Test
    @DisplayName("the sides of a MINUS are evaluated in the same graph")
    void minusWithSharedVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?a ?o WHERE {
                  GRAPH ?g { ?a :p ?o MINUS { ?a :p :o3 } }
                }"""), containsInAnyOrder(row(g1, s, o), row(g2, s, o), row(g2, s1, o1), row(g2, s2, o1),
                row(g2, s2, o2), row(g3, s, o)));
    }


    @Test
    @DisplayName("the graph variable occurring in a MINUS is a variable of its own")
    void minusWithGraphVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?a WHERE {
                  GRAPH ?g { ?a :p :o MINUS { ?b :q ?g } }
                }"""), containsInAnyOrder(row(g1, s), row(g2, s), row(g3, s)));
    }


    @Test
    @DisplayName("a MINUS without triples applies in every graph")
    void minusWithoutTriples() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?a ?o WHERE {
                  GRAPH ?g { ?a :p ?o MINUS { VALUES ?o { :o1 } } }
                }"""), containsInAnyOrder(row(g1, s, o), row(g1, s1, o2), row(g1, s1, o3), row(g2, s, o),
                row(g2, s2, o2), row(g3, s, o)));
    }


    @Test
    @DisplayName("a MINUS whose left side binds no graph is evaluated separately in every graph")
    void minusWithoutGraphOnLeft() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?x WHERE {
                  GRAPH ?g { VALUES ?x { :o2 :o3 } MINUS { ?s :p ?x } }
                }"""), containsInAnyOrder(row(g2, o3), row(g3, o2), row(g3, o3)));
    }


    @Test
    @DisplayName("an OPTIONAL whose left side binds no graph is evaluated separately in every graph")
    void optionalWithoutGraphOnLeft() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?x ?s WHERE {
                  GRAPH ?g { VALUES ?x { :o2 :o3 } OPTIONAL { ?s :p ?x } }
                }"""), containsInAnyOrder(row(g1, o2, s1), row(g1, o3, s1), row(g2, o2, s2), row(g2, o3, null),
                row(g3, o2, null), row(g3, o3, null)));
    }


    @Test
    @DisplayName("NOT EXISTS over an empty group is evaluated separately in every graph")
    void notExistsInEmptyGroup() throws Exception
    {
        assertThat(execute("""
                SELECT ?g WHERE {
                  GRAPH ?g { FILTER NOT EXISTS { ?s :p :o3 } }
                }"""), containsInAnyOrder(row(g2), row(g3)));
    }


    @Test
    @DisplayName("EXISTS next to a pattern binding no graph is evaluated separately in every graph")
    void existsWithoutGraphOnLeft() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?x WHERE {
                  GRAPH ?g { VALUES ?x { :o2 :o3 } FILTER EXISTS { ?s :p ?x } }
                }"""), containsInAnyOrder(row(g1, o2), row(g1, o3), row(g2, o2)));
    }


    @Test
    @DisplayName("a UNION binding the graph in one branch only is evaluated separately in every graph")
    void unionBindingGraphInOneBranch() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?s ?x WHERE {
                  GRAPH ?g { { ?s :p ?x } UNION { VALUES ?x { :o9 } } }
                }"""),
                containsInAnyOrder(row(g1, s, o), row(g1, s1, o1), row(g1, s1, o2), row(g1, s1, o3), row(g1, null, o9),
                        row(g2, s, o), row(g2, s1, o1), row(g2, s2, o1), row(g2, s2, o2), row(g2, null, o9),
                        row(g3, s, o), row(g3, null, o9)));
    }


    @Test
    @DisplayName("a sub-select in a pattern binding no graph is evaluated in the graph of the pattern")
    void subSelectInPatternWithoutGraph() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?y ?c WHERE {
                  GRAPH ?g { VALUES ?y { 1 } { SELECT (COUNT(*) AS ?c) { ?s :p ?o } } }
                }"""), containsInAnyOrder(row(g1, integer(1), integer(4)), row(g2, integer(1), integer(4)),
                row(g3, integer(1), integer(1))));
    }
}
