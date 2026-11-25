package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
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
 * The string constants of the generated SQL over connections whose setting {@code standard_conforming_strings} is off,
 * under which a backslash in a plain string constant would escape the next character: the engine writes a text holding
 * a backslash as an escape string constant, which means its text whatever the setting is, so a value holding
 * backslashes and quotes is neither altered nor able to end its constant. The table {@code item} holds such values,
 * mapped by ex:v. PostgreSQL 19 and newer do not accept the setting off, so the tests are skipped there.
 */
public class StandardConformingStringsTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Values holding backslashes and quotes.
     */
    private static final List<String> values = List.of("a\\b", "\\", "x\\'y", "\\n", "\\'); select 1; --");

    /**
     * True if the test database accepts the setting off, which PostgreSQL 19 and newer reject.
     */
    private static boolean accepted = false;

    /**
     * Pool of connections with the setting off.
     */
    private static DataSource legacyPool = null;

    /**
     * Engine over the pool of connections with the setting off.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code standard_conforming_strings_test} schema with the table of the values, the pool of connections
     * with the setting off, and the configuration mapping the table over that pool, when the test database accepts the
     * setting off.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource pool = Database.getPool();

        try(Connection connection = pool.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("show server_version_num"))
        {
            result.next();
            accepted = Integer.parseInt(result.getString(1)) < 190000;
        }

        if(!accepted)
            return;

        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema standard_conforming_strings_test");
            statement.execute(
                    "create table standard_conforming_strings_test.item (id int4 not null, v varchar not null)");

            try(PreparedStatement insert = connection
                    .prepareStatement("insert into standard_conforming_strings_test.item values (?, ?)"))
            {
                for(int i = 0; i < values.size(); i++)
                {
                    insert.setInt(1, i);
                    insert.setString(2, values.get(i));
                    insert.execute();
                }
            }
        }

        PoolConfiguration shared = pool.getPoolProperties();
        PoolProperties properties = new PoolProperties();
        properties.setUrl(shared.getUrl() + (shared.getUrl().contains("?") ? "&" : "?")
                + "options=-c%20standard_conforming_strings%3Doff");
        properties.setUsername(shared.getUsername());
        properties.setPassword(shared.getPassword());
        properties.setDriverClassName(shared.getDriverClassName());
        properties.setInitialSize(1);
        properties.setMinIdle(1);

        legacyPool = new DataSource(properties);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, legacyPool, new DatabaseSchema(pool),
                true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addQuadMapping(new DatabaseTable("standard_conforming_strings_test", "item"), null,
                config.createIriMapping("item", "id"), config.createIriMapping("ex:v"),
                config.createLiteralMapping(xsdString, "v"));

        engine = new Engine(config);
    }


    /**
     * Closes the pool of connections with the setting off, if it has been created.
     */
    @AfterAll
    static void close()
    {
        if(legacyPool != null)
            legacyPool.close();
    }


    /**
     * Skips the test when the test database does not accept the setting off.
     */
    @BeforeEach
    void requireSetting()
    {
        assumeTrue(accepted, "PostgreSQL 19 and newer do not accept standard_conforming_strings = off");
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declarations
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
     * The SPARQL string literal of the value.
     *
     * @param value the value
     * @return the SPARQL string literal of the value
     */
    private static String literal(String value)
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }


    @Test
    @DisplayName("the connections of the pool have the setting off")
    void legacyConnections() throws SQLException
    {
        try(Connection connection = legacyPool.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("show standard_conforming_strings"))
        {
            result.next();
            assertThat(result.getString(1), is("off"));
        }
    }


    @Test
    @DisplayName("string constants keep their backslashes and quotes")
    void constants() throws Exception
    {
        for(int i = 0; i < values.size(); i++)
        {
            String value = values.get(i);

            assertThat(value, execute("SELECT ?x WHERE { VALUES ?x { " + literal(value) + " } }"),
                    contains(List.<RdfTerm> of(new TypedLiteral(value, xsdStringIri))));

            assertThat(value, execute("SELECT ?s WHERE { ?s ex:v " + literal(value) + " }"),
                    contains(List.<RdfTerm> of(new Iri("http://example.org/item/" + i))));
        }
    }
}
