package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.MapUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * IRIs of user classes held as text and converted to the classes in the database, where the regular expression of a
 * class decides whether the text is an IRI of the class: an IRI that ends with a line feed is no member, though the
 * {@code $} of PCRE2 matches before a final line feed and the extraction of the id cuts the line feed off (or reads a
 * shifted id when the IRIs have a suffix, which then failed as no number). The current solution of an EXISTS pattern
 * takes this way into a sub-select, whose table accesses are restricted by the converted value.
 *
 * The table {@code raw} holds, for every class (integers without padding, of a fixed width and with a suffix, strings
 * of a fixed length and with a suffix, a mapped id of a fixed length with a suffix), one IRI of a member with a value
 * (the odd rows) and the same IRI with a final line feed (the even rows).
 */
public class IriRegexpQueryTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code iri_regexp_query_test} schema with the table {@code raw} of the IRIs and a table of values for
     * every class, and maps the IRIs by :raw and the values by :value.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "iri_regexp_query_test.";

            statement.execute("create schema iri_regexp_query_test");
            statement.execute("create table " + tables + "raw (l int primary key, t varchar not null)");
            statement.execute("insert into " + tables + "raw values "
                    + "(1, 'http://example.org/item/1'), (2, E'http://example.org/item/1\\n'), "
                    + "(3, 'http://example.org/fi/007'), (4, E'http://example.org/fi/007\\n'), "
                    + "(5, 'http://example.org/si/1/x.y'), (6, E'http://example.org/si/1/x.y\\n'), "
                    + "(7, 'http://example.org/fs/abcd'), (8, E'http://example.org/fs/abcd\\n'), "
                    + "(9, 'http://example.org/ss/ab__X'), (10, E'http://example.org/ss/ab_X\\n'), "
                    + "(11, 'http://example.org/fm/ABC_C'), (12, E'http://example.org/fm/ABC_C\\n')");

            for(String table : List.of("item", "fi", "si", "fm"))
            {
                statement.execute("create table " + tables + table + " (id int not null, v int not null)");
                statement.execute("insert into " + tables + table + " values (1, 10), (7, 70)");
            }

            for(String table : List.of("fs", "ss"))
            {
                statement.execute("create table " + tables + table + " (id varchar not null, v int not null)");
                statement.execute("insert into " + tables + table + " values ('abcd', 1), ('ab_', 2)");
            }

            statement.execute("create table " + tables + "fmap (id int primary key, code varchar not null)");
            statement.execute("insert into " + tables + "fmap values (7, 'ABC')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the tables.
     *
     * @param connectionPool pool of the test database
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");

        String ns = "http://example.org/";
        DatabaseTable map = new DatabaseTable("iri_regexp_query_test", "fmap");

        UserIriClass lnk = new IntegerUserIriClass("lnk", INT4, ns + "lnk/");
        List<UserIriClass> classes = List.of(new IntegerUserIriClass("item", INT4, ns + "item/"),
                new IntegerUserIriClass("fi", INT4, ns + "fi/", 3),
                new IntegerUserIriClass("si", INT4, ns + "si/", "/x.y"), new StringUserIriClass("fs", ns + "fs/", 4),
                new StringUserIriClass("ss", ns + "ss/", null, "_X"),
                new MapUserIriClass("fm", INT4, map, new TableColumn("id", INT4), new TableColumn("code", VARCHAR),
                        ns + "fm/", 3, "[A-Z]{3}", "_C"));

        configuration.addIriClass(lnk);
        configuration.addQuadMapping(new DatabaseTable("iri_regexp_query_test", "raw"), null,
                configuration.createIriMapping(lnk, "l"), configuration.createIriMapping("ex:raw"),
                configuration.createIriMapping(BuiltinClasses.iri, "t"));

        for(UserIriClass iriClass : classes)
        {
            configuration.addIriClass(iriClass);
            configuration.addQuadMapping(new DatabaseTable("iri_regexp_query_test", iriClass.getResourceName()), null,
                    configuration.createIriMapping(iriClass, "id"), configuration.createIriMapping("ex:value"),
                    configuration.createLiteralMapping(xsdInt, "v"));
        }

        configuration.setEngineSettings(configuration.getEngineSettings()
                .withJoinOptimizer(joinOptimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
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
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Rows of the IRIs of the table {@code raw}, one IRI each.
     *
     * @param rows numbers of the rows
     * @return rows of the IRIs
     */
    private static Object[] rows(int... rows)
    {
        return Arrays.stream(rows).mapToObj(r -> List.<RdfTerm> of(new Iri("http://example.org/lnk/" + r))).toArray();
    }


    @Test
    @DisplayName("an IRI with a final line feed counts no values of a member")
    void countedValues() throws Exception
    {
        String counted = "{ { SELECT (COUNT(*) AS ?c) WHERE { ?t :value ?v } } FILTER(?c > 0) }";

        assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER EXISTS " + counted + " }"),
                containsInAnyOrder(rows(1, 3, 5, 7, 9, 11)));

        // the same without the sub-select, which compares the values in a common class
        assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER EXISTS { ?t :value ?v } }"),
                containsInAnyOrder(rows(1, 3, 5, 7, 9, 11)));
    }


    @Test
    @DisplayName("an IRI with a final line feed finds no values of a member")
    void missingValues() throws Exception
    {
        assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER NOT EXISTS { { SELECT ?v WHERE { ?t :value ?v } } } }"),
                containsInAnyOrder(rows(2, 4, 6, 8, 10, 12)));
    }
}
