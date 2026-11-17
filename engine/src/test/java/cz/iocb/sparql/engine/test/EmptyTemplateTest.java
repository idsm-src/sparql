package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * A CONSTRUCT query whose template holds no triple, given or taken from an empty pattern: it constructs the empty
 * graph, whatever solutions its pattern has and whatever fetch size the results are read by.
 */
public class EmptyTemplateTest
{
    /**
     * Engine over a configuration mapping nothing.
     */
    private static Engine engine = null;


    /**
     * Creates the engine.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(),
                new DatabaseSchema(Database.getPool()), true));
    }


    /**
     * Triples constructed by the query, read by the given fetch size.
     *
     * @param query the query
     * @param fetchSize the fetch size
     * @return triples constructed by the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query, int fetchSize)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query, null, 0, -1, fetchSize, 0))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    @Test
    @DisplayName("a CONSTRUCT query of an empty template constructs the empty graph")
    void emptyTemplate() throws Exception
    {
        for(int fetchSize : List.of(0, 100))
        {
            assertThat(execute("CONSTRUCT {} WHERE {}", fetchSize), empty());
            assertThat(execute("CONSTRUCT {} WHERE { VALUES ?x { 1 2 } }", fetchSize), empty());
            assertThat(execute("CONSTRUCT {} WHERE { VALUES ?x { 1 2 } } LIMIT 1", fetchSize), empty());
            assertThat(execute("CONSTRUCT WHERE {}", fetchSize), empty());
        }
    }
}
