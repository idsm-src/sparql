package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
 * Evaluation of EXISTS for the current solution (SPARQL 1.2): the pattern of an EXISTS is evaluated with the solution
 * the filter is being evaluated on, so every group graph pattern inside it (the right sides of OPTIONAL and MINUS, the
 * branches of a UNION, the pattern of a GRAPH and the WHERE clause of a sub-select included) is joined with the current
 * solution and its filters and binds see the current values, also of variables the pattern binds nowhere.
 */
public class ExistsScopeTest
{
    private static final String prefix = "PREFIX : <http://example.org/> ";
    private static final Iri g1 = iri("g1");
    private static final Iri g2 = iri("g2");
    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");
    private static final Iri r = iri("r");

    private static DataSource connectionPool = null;
    private static DatabaseSchema schema = null;
    private static SparqlDatabaseConfiguration config = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();
        schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        quad(null, a, p, 1);
        quad(null, a, q, 1);
        quad(null, a, q, 2);
        quad(null, a, r, b);
        quad(null, b, p, 3);
        quad(null, b, q, 4);
        quad(null, b, q, 5);
        quad(null, c, p, 2);
        quad(g1, a, p, 1);
        quad(g1, b, p, 3);
        quad(g1, b, q, 4);
        quad(g2, a, p, 1);
        quad(g2, a, q, 1);
    }


    /**
     * Adds a constant quad with an IRI object to the configuration.
     *
     * @param graph the graph, null for the default graph
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     */
    private static void quad(Iri graph, Iri subject, Iri predicate, Iri object)
    {
        quad(graph, subject, predicate, new ConstantIriMapping(object));
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
        quad(graph, subject, predicate, config.createLiteralMapping(xsdInteger, integer(object)));
    }


    /**
     * Adds a constant quad to the configuration.
     *
     * @param graph the graph, null for the default graph
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object mapping
     */
    private static void quad(Iri graph, Iri subject, Iri predicate, TermMapping object)
    {
        config.addQuadMapping(graph == null ? null : new ConstantIriMapping(graph), new ConstantIriMapping(subject),
                new ConstantIriMapping(predicate), object);
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
    @DisplayName("a variable of the current solution is bound inside EXISTS even if the pattern does not bind it")
    void boundVariable() throws Exception
    {
        assertThat(execute("""
                SELECT * WHERE {
                  VALUES ?v { 1 2 3 }
                  FILTER EXISTS { FILTER bound(?v) }
                }"""), containsInAnyOrder(row(integer(1)), row(integer(2)), row(integer(3))));
    }


    @Test
    @DisplayName("a filter inside NOT EXISTS has the value of a variable of the current solution")
    void innerFilter() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER NOT EXISTS { ?x :q ?m FILTER(?n = ?m) }
                }"""), containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
    }


    @Test
    @DisplayName("a filter is the only element of the EXISTS pattern")
    void filterAlone() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { FILTER(?n > 1) }
                }"""), containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
    }


    @Test
    @DisplayName("the filters of a group apply to the whole group, so a triple after the EXISTS binds its variables")
    void filterBeforeTriple() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  FILTER EXISTS { ?x :q ?m FILTER(?m = ?n) }
                  ?x :p ?n
                }"""), containsInAnyOrder(row(a, integer(1))));
    }


    @Test
    @DisplayName("a nested EXISTS sees the solution of the enclosing EXISTS pattern, including the current one")
    void nested() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m FILTER EXISTS { FILTER(?m > ?n) } }
                }"""), containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
    }


    @Test
    @DisplayName("a nested group inside EXISTS starts from the current solution, not from the enclosing group")
    void nestedGroup() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m { FILTER(?m > ?n) } }
                }"""), empty());

        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m { FILTER(?n > 2) } }
                }"""), containsInAnyOrder(row(b, integer(3))));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL inside EXISTS sees the current solution")
    void optional() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m OPTIONAL { ?x :r ?y FILTER(?n = 1) } FILTER(bound(?y)) }
                }"""), containsInAnyOrder(row(a, integer(1))));
    }


    @Test
    @DisplayName("the sides of a MINUS inside EXISTS share the variables of the current solution")
    void minus() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?y :q ?m MINUS { ?z :p 1 } }
                }"""), empty());

        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?y :q ?m MINUS { ?z :p 7 } }
                }"""), containsInAnyOrder(row(a, integer(1)), row(b, integer(3)), row(c, integer(2))));
    }


    @Test
    @DisplayName("every branch of a UNION inside EXISTS sees the current solution")
    void union() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { { ?x :q ?m FILTER(?m > ?n) } UNION { FILTER(?n > 2) } }
                }"""), containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
    }


    @Test
    @DisplayName("the pattern of a GRAPH inside EXISTS sees the current solution")
    void graph() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { GRAPH ?g { ?x :q ?m FILTER(?m = ?n) } }
                }"""), containsInAnyOrder(row(a, integer(1))));
    }


    @Test
    @DisplayName("an EXISTS inside GRAPH ?g is evaluated in the graph of the current solution")
    void graphVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?g ?x WHERE {
                  GRAPH ?g { ?x :p ?n FILTER EXISTS { ?x :q ?m } }
                }"""), containsInAnyOrder(row(g1, b), row(g2, a)));
    }


    @Test
    @DisplayName("a sub-select standing for the EXISTS pattern is joined with the current solution")
    void subSelect() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { SELECT ?x WHERE { ?x :q ?m FILTER(?m > 4) } }
                }"""), containsInAnyOrder(row(b, integer(3))));
    }


    @Test
    @DisplayName("the WHERE clause of a sub-select inside EXISTS sees the current solution")
    void subSelectWhereClause() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { { SELECT (COUNT(*) AS ?c) WHERE { ?y :q ?m FILTER(?m > ?n) } } FILTER(?c = 2) }
                }"""), containsInAnyOrder(row(b, integer(3)), row(c, integer(2))));
    }


    @Test
    @DisplayName("a variable of the current solution is seen inside a sub-select even if it is not projected")
    void subSelectHiddenVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { { SELECT ?m WHERE { ?x :q ?m } } FILTER(?m = 5) }
                }"""), containsInAnyOrder(row(b, integer(3))));
    }


    @Test
    @DisplayName("a variable of the EXISTS pattern is not seen inside a sub-select that does not project it")
    void subSelectPatternVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m { SELECT ?k WHERE { ?y :q ?k FILTER(?k = ?m) } } }
                }"""), empty());

        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m { SELECT ?k WHERE { ?y :q ?k FILTER(?k = ?m + ?n) } } }
                }"""), empty());
    }


    @Test
    @DisplayName("a BIND inside EXISTS uses the current solution")
    void bind() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { BIND(?n + 1 AS ?k) FILTER(?k = 3) }
                }"""), containsInAnyOrder(row(c, integer(2))));
    }


    @Test
    @DisplayName("a BIND inside EXISTS to a variable of the current solution has to agree with its value")
    void bindOfCurrentVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { BIND(2 AS ?n) }
                }"""), containsInAnyOrder(row(c, integer(2))));

        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { BIND(1/0 AS ?n) }
                }"""), containsInAnyOrder(row(a, integer(1)), row(b, integer(3)), row(c, integer(2))));
    }


    @Test
    @DisplayName("a VALUES inside EXISTS of a variable of the current solution is joined with its value")
    void valuesOfCurrentVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { VALUES ?n { 1 3 } }
                }"""), containsInAnyOrder(row(a, integer(1)), row(b, integer(3))));
    }


    @Test
    @DisplayName("a property path inside EXISTS sees the current solution")
    void propertyPath() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  ?x :p ?n
                  FILTER EXISTS { ?x :r/:p ?k FILTER(?k > ?n) }
                }"""), containsInAnyOrder(row(a, integer(1))));
    }


    @Test
    @DisplayName("an EXISTS in the filter of an OPTIONAL sees the solution of both sides")
    void existsInOptionalFilter() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n ?m WHERE {
                  ?x :p ?n
                  OPTIONAL { ?x :q ?m FILTER EXISTS { FILTER(?m > ?n) } }
                }"""), containsInAnyOrder(row(a, integer(1), integer(2)), row(b, integer(3), integer(4)),
                row(b, integer(3), integer(5)), row(c, integer(2), null)));
    }


    @Test
    @DisplayName("an EXISTS in a select expression sees the current solution")
    void existsInSelectExpression() throws Exception
    {
        assertThat(execute("""
                SELECT ?x (EXISTS { ?x :q ?m FILTER(?m = ?n) } AS ?e) WHERE {
                  ?x :p ?n
                }"""), containsInAnyOrder(row(a, bool(true)), row(b, bool(false)), row(c, bool(false))));
    }


    @Test
    @DisplayName("an EXISTS in a HAVING clause sees the group solution")
    void existsInHaving() throws Exception
    {
        assertThat(execute("""
                SELECT ?x (COUNT(*) AS ?c) WHERE {
                  ?x :q ?m
                }
                GROUP BY ?x
                HAVING EXISTS { ?x :p 1 }"""), containsInAnyOrder(row(a, integer(2))));
    }


    @Test
    @DisplayName("a variable unbound in the current solution does not restrict the EXISTS pattern")
    void unboundVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n ?y WHERE {
                  ?x :p ?n
                  OPTIONAL { ?x :r ?y }
                  FILTER EXISTS { ?y :q 4 }
                }"""), containsInAnyOrder(row(a, integer(1), b), row(b, integer(3), null), row(c, integer(2), null)));
    }


    @Test
    @DisplayName("a constant of the current solution is used inside EXISTS")
    void constantVariable() throws Exception
    {
        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  VALUES ?x { :a }
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m FILTER(?m = ?n) }
                }"""), containsInAnyOrder(row(a, integer(1))));

        assertThat(execute("""
                SELECT ?x ?n WHERE {
                  VALUES ?x { :b }
                  ?x :p ?n
                  FILTER EXISTS { ?x :q ?m FILTER(?m = ?n) }
                }"""), empty());
    }


    @Test
    @DisplayName("an empty EXISTS pattern has the current solution")
    void emptyPattern() throws Exception
    {
        assertThat(execute("""
                SELECT ?x WHERE {
                  ?x :p ?n
                  FILTER EXISTS { }
                }"""), containsInAnyOrder(row(a), row(b), row(c)));

        assertThat(execute("""
                SELECT ?x WHERE {
                  ?x :p ?n
                  FILTER NOT EXISTS { }
                }"""), empty());
    }
}
