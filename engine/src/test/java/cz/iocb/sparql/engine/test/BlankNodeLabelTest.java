package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import cz.iocb.sparql.engine.error.MessageCategory;
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
 * The scope of the labels of blank nodes. A label can be used in one basic graph pattern of a query only. A basic graph
 * pattern is a sequence of triple patterns with optional filters: a filter does not end it, also when the filter holds
 * a pattern of its own, which is another basic graph pattern, whereas every other pattern ends it.
 */
public class BlankNodeLabelTest
{
    private static final String prefix = "PREFIX : <http://example.org/> ";
    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");
    private static final Iri r = iri("r");

    private static SparqlDatabaseConfiguration config = null;
    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), false);

        quad(a, p, config.createLiteralMapping(xsdInteger, integer(1)));
        quad(a, q, config.createLiteralMapping(xsdInteger, integer(1)));
        quad(a, q, config.createLiteralMapping(xsdInteger, integer(2)));
        quad(a, r, new ConstantIriMapping(b));
        quad(b, p, config.createLiteralMapping(xsdInteger, integer(3)));
        quad(b, q, config.createLiteralMapping(xsdInteger, integer(4)));
        quad(b, q, config.createLiteralMapping(xsdInteger, integer(5)));
        quad(c, p, config.createLiteralMapping(xsdInteger, integer(2)));

        engine = new Engine(config);
    }


    /**
     * Adds a constant triple of the default graph to the configuration.
     *
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object mapping
     */
    private static void quad(Iri subject, Iri predicate, TermMapping object)
    {
        config.addQuadMapping(null, new ConstantIriMapping(subject), new ConstantIriMapping(predicate), object);
    }


    /**
     * Texts of the error messages of the query.
     *
     * @param query the query, without the prefix declaration
     * @return texts of the error messages of the query
     * @throws SQLException on database errors
     */
    private static List<String> errors(String query) throws SQLException
    {
        try(Request request = engine.getRequest())
        {
            return request.check(prefix + query).stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                    .map(TranslateMessage::getMessage).toList();
        }
    }


    /**
     * The error message of a label used in two basic graph patterns.
     *
     * @param label the label
     * @return the error message of a label used in two basic graph patterns
     */
    private static String reuse(String label)
    {
        return "Blank node '" + label + "' is already used in another basic graph pattern.";
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


    @Test
    @DisplayName("a filter does not end the basic graph pattern of a label")
    void filters() throws Exception
    {
        for(String pattern : List.of("_:a :p ?n FILTER(?n < 3) _:a :r ?y",
                "_:a :p ?n FILTER EXISTS { ?s :q ?n } _:a :r ?y",
                "_:a :p ?n FILTER NOT EXISTS { ?s :q ?n } FILTER(?n < 3) . _:a :r ?y",
                "_:a :p ?n FILTER(?n < 3) _:a :r ?y FILTER EXISTS { ?s :q ?n } . _:a :q ?m",
                "_:a :p ?n FILTER EXISTS { _:b :q ?n FILTER EXISTS { ?s :r ?t } _:b :p ?m } _:a :r ?y"))
            assertEquals(List.of(), errors("SELECT * WHERE { " + pattern + " }"), pattern);
    }


    @Test
    @DisplayName("the pattern of a filter is another basic graph pattern")
    void filterPatterns() throws Exception
    {
        for(String pattern : List.of("_:a :p ?n FILTER EXISTS { _:a :q ?n }", "FILTER EXISTS { _:a :q ?n } _:a :p ?n",
                "?x :p ?n FILTER EXISTS { _:a :q ?n } FILTER EXISTS { _:a :q ?m }",
                "?x :p ?n FILTER(EXISTS { _:a :q ?n } || EXISTS { _:a :q ?m })",
                "?x :p ?n FILTER EXISTS { ?y :q ?n FILTER EXISTS { _:a :r ?t } _:a :p ?m }",
                "_:a :p ?n FILTER EXISTS { ?y :q ?n FILTER EXISTS { _:a :r ?t } } _:a :r ?y"))
            assertEquals(List.of(reuse("_:a")), errors("SELECT * WHERE { " + pattern + " }"), pattern);
    }


    @Test
    @DisplayName("a pattern other than a filter ends the basic graph pattern of a label")
    void otherPatterns() throws Exception
    {
        for(String pattern : List.of("OPTIONAL { ?s :q ?m }", "MINUS { ?s :q ?m }", "{ ?s :q ?m }",
                "{ ?s :q ?m } UNION { ?s :p ?m }", "GRAPH ?g { ?s :q ?m }", "{ SELECT ?s WHERE { ?s :q ?m } }",
                "BIND(1 AS ?m)", "VALUES ?m { 1 }", "FILTER EXISTS { ?s :q ?n } OPTIONAL { ?s :q ?m }"))
            assertEquals(List.of(reuse("_:a")), errors("SELECT * WHERE { _:a :p ?n " + pattern + " _:a :r ?y }"),
                    pattern);
    }


    @Test
    @DisplayName("a label joins the triple patterns around a filter")
    void evaluation() throws Exception
    {
        assertThat(execute("SELECT * WHERE { _:a :p ?n FILTER EXISTS { _:b :q ?n } _:a :r ?y }"),
                contains(row(integer(1), b)));
        assertThat(execute("SELECT * WHERE { _:a :p ?n FILTER NOT EXISTS { _:b :q ?n } _:a :q ?m }"),
                containsInAnyOrder(row(integer(3), integer(4)), row(integer(3), integer(5))));
    }
}
