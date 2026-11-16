package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * An OPTIONAL whose right side changes no value its parent reads, where duplicate solutions do not matter: inside an
 * EXISTS pattern, under DISTINCT. Every variable the right side binds that the parent needs is bound always by the left
 * side or by the current solution of the EXISTS pattern, which every solution of the right side extends, so every
 * solution of the left side stays a solution, extended at most by values nobody reads, and the OPTIONAL is left out.
 * Left out at the start of an EXISTS pattern, it no longer makes the test read the current solution, so the test is
 * evaluated at once and not for every solution anew. The OPTIONAL stays where a filter reads its variable, where a
 * later member of the group joins it on a variable the current solution may leave unbound, and where the number of
 * solutions matters.
 *
 * The table {@code edge} holds the edges 2 -&gt; 1, 3 -&gt; 1, 4 -&gt; 2, 5 -&gt; 2 and 6 -&gt; 3 between items; the
 * table {@code name} holds the names of the items 1, 2 (two of them) and 4.
 */
public class UnreadOptionalTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * The table of the names as the generated SQL names it.
     */
    private static final String names = "\"unread_optional_test\".\"name\"";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code unread_optional_test} schema with the tables {@code edge} and {@code name} and maps the edges
     * by ex:sub and the names by ex:name.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema unread_optional_test");
            statement.execute("create table unread_optional_test.edge (s int not null, o int not null)");
            statement.execute("insert into unread_optional_test.edge values (2, 1), (3, 1), (4, 2), (5, 2), (6, 3)");
            statement.execute("create table unread_optional_test.name (s int not null, l varchar not null)");
            statement.execute("insert into unread_optional_test.name values (1, 'one'), (2, 'two'), (2, 'zwei'), "
                    + "(4, 'four')");
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
        configuration.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable edge = new DatabaseTable("unread_optional_test", "edge");
        DatabaseTable name = new DatabaseTable("unread_optional_test", "name");
        TermMapping subject = configuration.createIriMapping("item", "s");

        configuration.addQuadMapping(edge, null, subject, configuration.createIriMapping("ex:sub"),
                configuration.createIriMapping("item", "o"));
        configuration.addQuadMapping(name, null, subject, configuration.createIriMapping("ex:name"),
                configuration.createLiteralMapping(xsdString, "l"));
        configuration.setJoinOptimizer(joinOptimizer);

        return configuration;
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = new Engine(config).getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
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
     * Rows of the items, one item each.
     *
     * @param ids identifiers of the items
     * @return rows of the items
     */
    private static Object[] items(int... ids)
    {
        return Arrays.stream(ids).mapToObj(id -> List.<RdfTerm> of(new Iri("http://example.org/item/" + id))).toArray();
    }


    @Test
    @DisplayName("an OPTIONAL at the start of an EXISTS pattern")
    void leadingOptionalInExists() throws Exception
    {
        String pattern = "{ OPTIONAL { ?b ex:name ?c } ?b ex:sub* item:2 }";

        assertThat(execute("SELECT ?a WHERE { ?a ex:sub ?b FILTER EXISTS " + pattern + " }"),
                containsInAnyOrder(items(4, 5)));

        assertThat(execute("SELECT ?a WHERE { ?a ex:sub ?b FILTER NOT EXISTS " + pattern + " }"),
                containsInAnyOrder(items(2, 3, 6)));

        assertThat(translate("SELECT ?a WHERE { ?a ex:sub ?b FILTER EXISTS " + pattern + " }"),
                not(containsString(names)));
    }


    @Test
    @DisplayName("an OPTIONAL extending the solutions of an EXISTS pattern")
    void optionalInExists() throws Exception
    {
        String query = "SELECT ?a WHERE { ?a ex:sub ?b FILTER EXISTS { ?b ex:sub ?d OPTIONAL { ?d ex:name ?c } } }";

        assertThat(execute(query), containsInAnyOrder(items(4, 5, 6)));
        assertThat(translate(query), not(containsString(names)));
    }


    @Test
    @DisplayName("an OPTIONAL under DISTINCT, and the same OPTIONAL where the number of solutions matters")
    void distinct() throws Exception
    {
        String pattern = "{ ?a ex:sub ?b OPTIONAL { ?b ex:name ?c } }";

        assertThat(execute("SELECT DISTINCT ?a WHERE " + pattern), containsInAnyOrder(items(2, 3, 4, 5, 6)));
        assertThat(translate("SELECT DISTINCT ?a WHERE " + pattern), not(containsString(names)));

        // the item 2 has two names, so the items 4 and 5 below it are two solutions each
        assertThat(execute("SELECT ?a WHERE " + pattern), containsInAnyOrder(items(2, 3, 4, 4, 5, 5, 6)));

        assertThat(execute("SELECT (COUNT(*) AS ?n) WHERE " + pattern),
                contains(List.of(new TypedLiteral("7", xsdIntegerIri))));
    }


    @Test
    @DisplayName("an OPTIONAL whose variable a filter reads stays")
    void readByFilter() throws Exception
    {
        String query = "SELECT ?a WHERE { ?a ex:sub ?b FILTER EXISTS { OPTIONAL { ?b ex:name ?c } FILTER(BOUND(?c)) } }";

        assertThat(execute(query), containsInAnyOrder(items(2, 3, 4, 5)));
        assertThat(translate(query), containsString(names));
    }


    @Test
    @DisplayName("an OPTIONAL binding a variable the current solution may leave unbound stays")
    void unboundCurrentValue() throws Exception
    {
        // without a value of ?x, the names bind it to items that are not below the item 3, so the group has no
        // solution; with ?x = item:6, which has no name, the OPTIONAL leaves the solution as it is
        assertThat(
                execute("SELECT ?k WHERE { VALUES (?k ?x) { (1 UNDEF) (2 item:6) } "
                        + "FILTER EXISTS { OPTIONAL { ?x ex:name ?l } ?x ex:sub item:3 } }"),
                contains(List.of(new TypedLiteral("2", xsdIntegerIri))));
    }
}
