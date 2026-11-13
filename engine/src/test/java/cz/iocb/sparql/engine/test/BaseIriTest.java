package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
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
 * The base IRI of a query. The relative IRIs of a query and the argument of the function IRI are resolved against the
 * base IRI in effect, as section 5.2 of RFC 3986 says: that of the last BASE declaration, and before it the IRI of the
 * service of the configuration, which is the default base IRI of its queries. When there is neither, a relative IRI is
 * an error of the query and the function IRI gives nothing for a relative reference, because it has to result in an
 * absolute IRI.
 */
public class BaseIriTest
{
    /**
     * Engine of a configuration without a service IRI, so without a default base IRI.
     */
    private static Engine plain = null;

    /**
     * Engine of a configuration with the service IRI {@code http://example.org/service/endpoint}.
     */
    private static Engine served = null;


    @BeforeAll
    static void init() throws SQLException
    {
        plain = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
        served = new Engine(new SparqlDatabaseConfiguration("http://example.org/service/endpoint", Database.getPool(),
                null, false));
    }


    /**
     * Rows of the query.
     *
     * @param engine the engine to run the query
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(Engine engine, String query)
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
     * Texts of the error messages of the query.
     *
     * @param engine the engine to check the query
     * @param query the query
     * @return texts of the error messages of the query
     * @throws SQLException on database errors
     */
    private static List<String> errors(Engine engine, String query) throws SQLException
    {
        try(Request request = engine.getRequest())
        {
            return request.check(query).stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                    .map(TranslateMessage::getMessage).toList();
        }
    }


    /**
     * The error message of a relative IRI that has no base IRI.
     *
     * @param iri the relative IRI
     * @return the error message of a relative IRI that has no base IRI
     */
    private static String unresolved(String iri)
    {
        return "Relative IRI '" + iri + "' cannot be resolved, because there is no base IRI.";
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
     * Row of the IRIs.
     *
     * @param iris the IRIs
     * @return row of the IRIs
     */
    private static List<RdfTerm> iris(String... iris)
    {
        return Arrays.stream(iris).<RdfTerm> map(Iri::new).toList();
    }


    /**
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Row of a reference and of the IRI that both IRI and URI resolve it to.
     *
     * @param reference the IRI reference
     * @param iri the resolved IRI
     * @return row of the reference and of the IRI twice
     */
    private static List<RdfTerm> resolved(String reference, String iri)
    {
        return row(string(reference), new Iri(iri), new Iri(iri));
    }


    @Test
    @DisplayName("a relative IRI is resolved against the BASE of the query")
    void declaredBase() throws Exception
    {
        assertThat(execute(plain, """
                BASE <http://example.org/base/doc?old#old> \
                SELECT (<data> AS ?a) (<../up> AS ?b) (<#part> AS ?c) (<> AS ?d) (<?query> AS ?e) \
                (</root> AS ?f) (<//other.org/data> AS ?g) WHERE { }"""),
                contains(iris("http://example.org/base/data", "http://example.org/up",
                        "http://example.org/base/doc?old#part", "http://example.org/base/doc?old",
                        "http://example.org/base/doc?query", "http://example.org/root", "http://other.org/data")));

        // also the IRI of a prefix and the datatype of a literal
        assertThat(
                execute(plain,
                        "BASE <http://example.org/base/> PREFIX p: <ns/> "
                                + "SELECT (p:x AS ?a) (DATATYPE(\"1\"^^<type>) AS ?b) WHERE { }"),
                contains(iris("http://example.org/base/ns/x", "http://example.org/base/type")));

        // an IRI that is not relative is taken as it is written
        assertThat(
                execute(plain, "BASE <http://example.org/base/> SELECT (<http://example.org/a/../b> AS ?a) WHERE { }"),
                contains(iris("http://example.org/a/../b")));
    }


    @Test
    @DisplayName("a BASE declaration is in effect for what follows it")
    void laterBase() throws Exception
    {
        assertThat(
                execute(plain,
                        "BASE <http://example.org/one/> PREFIX p: <ns/> BASE <http://example.org/two/> "
                                + "SELECT (p:x AS ?a) (<x> AS ?b) (IRI(\"x\") AS ?c) WHERE { }"),
                contains(iris("http://example.org/one/ns/x", "http://example.org/two/x", "http://example.org/two/x")));

        assertEquals(List.of(unresolved("ns/")),
                errors(plain, "PREFIX p: <ns/> BASE <http://example.org/> SELECT (p:x AS ?a) WHERE { }"));
        assertEquals(List.of("Base IRI must be absolute."), errors(plain, "BASE <base/> SELECT * WHERE { }"));
    }


    @Test
    @DisplayName("the IRI of the service is the base IRI of a query that declares none")
    void defaultBase() throws Exception
    {
        assertThat(execute(served, "SELECT (<data> AS ?a) (<> AS ?b) (<#part> AS ?c) (IRI(\"data\") AS ?d) WHERE { }"),
                contains(iris("http://example.org/service/data", "http://example.org/service/endpoint",
                        "http://example.org/service/endpoint#part", "http://example.org/service/data")));
        assertThat(
                execute(served,
                        "BASE <http://example.org/base/> SELECT (<data> AS ?a) (IRI(\"data\") AS ?b) WHERE { }"),
                contains(iris("http://example.org/base/data", "http://example.org/base/data")));
    }


    @Test
    @DisplayName("a relative IRI is an error when there is no base IRI")
    void noBase() throws Exception
    {
        assertEquals(List.of(unresolved("data")), errors(plain, "SELECT (<data> AS ?a) WHERE { }"));
        assertEquals(List.of(unresolved("")), errors(plain, "SELECT * WHERE { <> ?p ?o }"));
        assertEquals(List.of(unresolved("ns/")), errors(plain, "PREFIX p: <ns/> SELECT (p:x AS ?a) WHERE { }"));
        assertEquals(List.of(unresolved("type")), errors(plain, "SELECT (\"1\"^^<type> AS ?a) WHERE { }"));
        assertEquals(List.of(unresolved("graph")), errors(plain, "SELECT * FROM <graph> WHERE { ?s ?p ?o }"));
        assertEquals(List.of(unresolved("//other.org/data")),
                errors(plain, "SELECT * WHERE { ?s ?p <//other.org/data> }"));

        assertThat(execute(plain, "SELECT (<http://example.org/data> AS ?a) (<urn:example:data> AS ?b) WHERE { }"),
                contains(iris("http://example.org/data", "urn:example:data")));

        // a service IRI that is not absolute is no base IRI
        Engine relative = new Engine(new SparqlDatabaseConfiguration("service", Database.getPool(), null, false));

        assertEquals(List.of(unresolved("data")), errors(relative, "SELECT (<data> AS ?a) WHERE { }"));
    }


    @Test
    @DisplayName("the function IRI resolves a reference against the base IRI of the query")
    void iriFunction() throws Exception
    {
        assertThat(
                execute(plain, "BASE <http://example.org/base/doc> SELECT ?s (IRI(?s) AS ?iri) (URI(?s) AS ?uri) WHERE "
                        + "{ VALUES ?s { \"data\" \"../up\" \"#part\" \"\" \"http://other.org/data\" \"a b\" } }"),
                containsInAnyOrder(resolved("data", "http://example.org/base/data"),
                        resolved("../up", "http://example.org/up"),
                        resolved("#part", "http://example.org/base/doc#part"),
                        resolved("", "http://example.org/base/doc"),
                        resolved("http://other.org/data", "http://other.org/data"), row(string("a b"), null, null)));
        assertThat(execute(served, "SELECT ?s (IRI(?s) AS ?iri) (URI(?s) AS ?uri) WHERE { VALUES ?s { \"data\" } }"),
                contains(resolved("data", "http://example.org/service/data")));
    }


    @Test
    @DisplayName("the function IRI gives no relative IRI when there is no base IRI")
    void iriFunctionWithoutBase() throws Exception
    {
        assertThat(
                execute(plain,
                        "SELECT ?s (IRI(?s) AS ?iri) (URI(?s) AS ?uri) WHERE "
                                + "{ VALUES ?s { \"data\" \"/root\" \"#part\" \"\" \"http://other.org/data\" } }"),
                containsInAnyOrder(row(string("data"), null, null), row(string("/root"), null, null),
                        row(string("#part"), null, null), row(string(""), null, null),
                        resolved("http://other.org/data", "http://other.org/data")));

        // nothing is built on a relative IRI
        assertThat(
                execute(plain,
                        "SELECT (BOUND(?iri) AS ?bound) (COALESCE(STRDT(\"1\", ?iri), \"none\") AS ?literal) "
                                + "WHERE { BIND(IRI(\"type\") AS ?iri) }"),
                contains(row(new TypedLiteral("false", xsdBooleanIri), string("none"))));

        // an IRI is returned as it is
        assertThat(execute(plain, "SELECT (IRI(<http://example.org/a/../b>) AS ?iri) WHERE { }"),
                contains(iris("http://example.org/a/../b")));
    }
}
