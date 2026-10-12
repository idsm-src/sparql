package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Optimisation of a join whose child is a remote SERVICE call, which is not evaluated in the first pass: the join adds
 * the variables of the call to the restrictions it has got, also when those already require a variable in any class, as
 * the restrictions of a filter do. The service is never called, because only the first pass is run.
 */
public class ServiceJoinRestrictionsTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code service_test} schema with the table {@code item} of generic IRIs and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_test");
            statement.execute("create table service_test.item (iri varchar collate \"C\" primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into service_test.item values ('http://example.org/item/1', 'one')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        DatabaseTable item = new DatabaseTable("service_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(item, graph, config.createIriMapping(BuiltinClasses.iri, "iri"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
    }


    /**
     * Runs the first optimisation pass of the query.
     *
     * @param query the query, without the prefix declaration
     * @return the optimised intermediate code
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static SqlIntercode optimize(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false);
        }
    }


    @Test
    @DisplayName("a filter on a variable of a SERVICE call joined with another pattern")
    void filteredJoinWithService() throws Exception
    {
        SqlIntercode imcode = optimize("""
                SELECT ?x ?o WHERE { ?x ex:label ?l . \
                SERVICE <http://example.org/remote/sparql> { ?x ex:remote ?o } ?o ex:label ?m \
                FILTER(?o != ex:none) }""");

        assertNotNull(imcode);
    }
}
