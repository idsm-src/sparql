package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The first optimisation pass of comparisons between variables of an unevaluated SERVICE call, which are bound in every
 * class of the configuration: with hundreds of IRI classes the combinations of their classes must not be compared one
 * by one, neither in time nor in the generated SQL.
 */
public class WideComparisonTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The SERVICE call binding {@code ?o} and {@code ?n}, never evaluated.
     */
    private static final String service = "SERVICE <http://example.org/remote/sparql> { ?x ex:p ?o ; ex:q ?n } ";

    /**
     * Engine over a configuration of two hundred IRI classes.
     */
    private static Engine engine = null;


    /**
     * Registers two hundred IRI classes of distinct prefixes.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(), null, false);
        config.addPrefix("ex", "http://example.org/");

        for(int i = 0; i < 200; i++)
            config.addIriClass(new IntegerUserIriClass("ex:c" + i, INT4, "http://example.org/c" + i + "/"));

        engine = new Engine(config);
    }


    /**
     * Runs the first optimisation pass of a query binding the expression over the SERVICE variables, within thirty
     * seconds.
     *
     * @param expression the expression over {@code ?o} and {@code ?n}
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static void optimize(String expression) throws SQLException, TranslateExceptions, ServiceException
    {
        String query = prefix + "SELECT ?x ?r WHERE { " + service + "BIND(" + expression + " AS ?r) }";

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            try(Request request = engine.getRequest())
            {
                PreparedQuery prepared = request.prepareQuery(query, null);

                SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null,
                        List.of());

                SqlIntercode optimized = imcode.optimize(request, false);

                assertNotNull(optimized);
            }
        });
    }


    @Test
    @DisplayName("equality and inequality of two SERVICE variables")
    void equality() throws Exception
    {
        optimize("?o = ?n");
        optimize("?o != ?n");
        optimize("?o < ?n");
    }


    @Test
    @DisplayName("IN and NOT IN over SERVICE variables")
    void membership() throws Exception
    {
        optimize("?o IN (1, \"a\", ex:x)");
        optimize("?o NOT IN (?n)");
    }
}
