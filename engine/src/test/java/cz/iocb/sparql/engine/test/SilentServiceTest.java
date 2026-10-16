package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.ServiceRuntimeException;
import cz.iocb.sparql.testing.Database;



/**
 * Failing calls of SERVICE SILENT: a call to an unreachable endpoint yields the solutions of its context with the
 * variables of the call unbound, also when the context holds an earlier call, which must not stay in the evaluated
 * query unevaluated; the same call without SILENT is an error. The endpoints are ports of the loopback address nothing
 * listens on, so no network is needed.
 */
public class SilentServiceTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Call of an unreachable endpoint binding {@code ?x} and {@code ?y}.
     */
    private static final String first = "SERVICE SILENT <http://127.0.0.1:1/sparql> { ?x ex:p ?y } ";

    /**
     * Call of another unreachable endpoint binding {@code ?z} for {@code ?y}.
     */
    private static final String second = "SERVICE SILENT <http://127.0.0.1:2/sparql> { ?y ex:q ?z } ";

    /**
     * The mapped item.
     */
    private static final Iri item = new Iri("http://example.org/item/1");

    /**
     * Label of the mapped item.
     */
    private static final TypedLiteral label = new TypedLiteral("one", xsdStringIri);

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code silent_service_test} schema with the table {@code item} of one labelled IRI and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema silent_service_test");
            statement.execute("create table silent_service_test.item (iri varchar collate \"C\" primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into silent_service_test.item values ('http://example.org/item/1', 'one')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        DatabaseTable table = new DatabaseTable("silent_service_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping(BuiltinClasses.iri, "iri"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));

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
    @DisplayName("a failing SILENT call yields its context with the variables of the call unbound")
    void silentCallWithLocalPattern() throws Exception
    {
        assertThat(execute("SELECT ?x ?l ?y WHERE { " + first + "?x ex:label ?l }"), contains(row(item, label, null)));
    }


    @Test
    @DisplayName("a failing SILENT call whose context is an earlier failing SILENT call")
    void chainedSilentCalls() throws Exception
    {
        assertThat(execute("SELECT ?x ?y ?z WHERE { " + first + second + "}"), contains(row(null, null, null)));
    }


    @Test
    @DisplayName("two failing SILENT calls joined with a local pattern")
    void chainedSilentCallsWithLocalPattern() throws Exception
    {
        assertThat(execute("SELECT ?x ?l ?y ?z WHERE { " + first + second + "?x ex:label ?l }"),
                contains(row(item, label, null, null)));
    }


    @Test
    @DisplayName("a failing call without SILENT is an error")
    void callWithoutSilentIsAnError()
    {
        assertThrows(ServiceRuntimeException.class,
                () -> execute("SELECT ?x ?y ?z WHERE { " + first + second.replace("SERVICE SILENT", "SERVICE") + "}"));
    }
}
