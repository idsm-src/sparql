package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.Database;
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



/**
 * The service description of a configuration: the SPARQL 1.2 language and version terms next to the SPARQL 1.1 one.
 */
public class ServiceDescriptionTest
{
    private static final String prefix = "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> ";
    private static final String sd = "http://www.w3.org/ns/sparql-service-description#";
    private static final String sparql = "http://www.w3.org/ns/sparql#";

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration("http://example.org/sparql",
                Database.getPool(), new DatabaseSchema(Database.getPool()), true);

        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("sd", sd);
        config.addPrefix("ent", "http://www.w3.org/ns/entailment/");
        config.addPrefix("format", "http://www.w3.org/ns/formats/");
        config.addBasicServiceDescription();

        engine = new Engine(config);
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
}
