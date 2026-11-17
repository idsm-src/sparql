package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The service description of a configuration: the SPARQL 1.2 language and version terms next to the SPARQL 1.1 one, the
 * terms given by their IRIs whatever prefixes the configuration registers (the configuration binds {@code format} to
 * another namespace than that of the formats and declares no other prefix), and a configuration without a service
 * having no description.
 */
public class ServiceDescriptionTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> ";

    /**
     * Namespace of the terms of the SPARQL service description.
     */
    private static final String sd = "http://www.w3.org/ns/sparql-service-description#";

    /**
     * Namespace of the versions of SPARQL.
     */
    private static final String sparql = "http://www.w3.org/ns/sparql#";

    /**
     * Namespace of the formats of the results.
     */
    private static final String formats = "http://www.w3.org/ns/formats/";

    /**
     * Local names of the formats of the results.
     */
    private static final List<String> formatNames = List.of("SPARQL_Results_XML", "SPARQL_Results_JSON",
            "SPARQL_Results_CSV", "SPARQL_Results_TSV", "RDF_JSON", "RDF_XML", "Turtle", "TriG", "N-Triples",
            "N-Quads");

    /**
     * Engine over the described configuration.
     */
    private static Engine engine = null;


    /**
     * Describes the service of a configuration that binds the prefix {@code format} to another namespace.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration("http://example.org/sparql",
                Database.getPool(), new DatabaseSchema(Database.getPool()), true);

        config.addPrefix("format", "http://example.org/formats/");
        config.addBasicServiceDescription();

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
     *
     * @param engine the engine
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
     * Rows of the query over the described configuration.
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
        return execute(engine, prefix + query);
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
    @DisplayName("the service supports the SPARQL query language in the versions 1.2, 1.2-basic and 1.1")
    void supportedLanguage() throws Exception
    {
        assertThat(execute("SELECT ?l WHERE { <http://example.org/sparql> sd:supportedLanguage ?l }"),
                containsInAnyOrder(row(new Iri(sd + "SPARQLQuery")), row(new Iri(sd + "SPARQL11Query"))));

        assertThat(execute("SELECT ?v WHERE { <http://example.org/sparql> sd:supportedVersion ?v }"),
                containsInAnyOrder(row(new Iri(sparql + "version-1.2")), row(new Iri(sparql + "version-1.2-basic")),
                        row(new Iri(sparql + "version-1.1"))));
    }


    @Test
    @DisplayName("the terms of the description are given by their IRIs, not by the prefixes of the configuration")
    void terms() throws Exception
    {
        assertThat(execute("SELECT ?t WHERE { <http://example.org/sparql> a ?t }"),
                containsInAnyOrder(row(new Iri(sd + "Service"))));

        assertThat(execute("SELECT ?e WHERE { <http://example.org/sparql> sd:defaultEntailmentRegime ?e }"),
                containsInAnyOrder(row(new Iri("http://www.w3.org/ns/entailment/Simple"))));

        assertThat(execute("SELECT ?f WHERE { <http://example.org/sparql> sd:resultFormat ?f }"),
                containsInAnyOrder(formatNames.stream().map(f -> row(new Iri(formats + f))).toArray()));
    }


    @Test
    @DisplayName("a configuration without a service has no description")
    void withoutService() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(),
                new DatabaseSchema(Database.getPool()), true);

        assertThrows(IllegalStateException.class, config::addBasicServiceDescription);
        assertThat(execute(new Engine(config), config.getServiceDescriptionQuery()), empty());
    }
}
