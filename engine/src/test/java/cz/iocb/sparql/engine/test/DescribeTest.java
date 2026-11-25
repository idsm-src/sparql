package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * DESCRIBE of a variable gives the triples whose subject or object is a term the solutions bind the variable to. The
 * solutions may leave the variable unbound, and the triples may hold it in classes the solutions never bind it in; the
 * description of the variable was then translated over the classes of the solutions alone and failed with a
 * {@code NullPointerException}.
 *
 * The table {@code link} holds the links (1, 2) and (2, 3) between items, mapped by ex:link, the table {@code ref} the
 * reference (1, 7) from an item to a thing of another IRI class, mapped by ex:ref.
 */
public class DescribeTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code describe_test} schema with the tables {@code link} and {@code ref}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema describe_test");
            statement.execute("create table describe_test.link (s int not null, o int not null)");
            statement.execute("insert into describe_test.link values (1, 2), (2, 3)");
            statement.execute("create table describe_test.ref (s int not null, o int not null)");
            statement.execute("insert into describe_test.ref values (1, 7)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the tables: the links between items of the class {@code item} by ex:link, and the
     * references from items to things of the class {@code other} by ex:ref.
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
        configuration.addIriClass(new IntegerUserIriClass("other", INT4, "http://example.org/other/"));

        TermMapping subject = configuration.createIriMapping("item", "s");

        configuration.addQuadMapping(new DatabaseTable("describe_test", "link"), null, subject,
                configuration.createIriMapping("ex:link"), configuration.createIriMapping("item", "o"));
        configuration.addQuadMapping(new DatabaseTable("describe_test", "ref"), null, subject,
                configuration.createIriMapping("ex:ref"), configuration.createIriMapping("other", "o"));
        configuration.setJoinOptimizer(joinOptimizer);

        return configuration;
    }


    /**
     * Triples of the description, which have to be the same with the join optimizer enabled.
     *
     * @param query the query, without the prefix declarations
     * @return triples of the description
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static Set<List<RdfTerm>> describe(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Set<List<RdfTerm>> triples = describe(config, query);

        assertThat(describe(optimized, query), is(triples));

        return triples;
    }


    /**
     * Triples of the description under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return triples of the description
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static Set<List<RdfTerm>> describe(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            Set<List<RdfTerm>> triples = new HashSet<>();

            while(result.next())
                triples.add(Arrays.asList(result.getRow()));

            return triples;
        }
    }


    /**
     * Triple of ex:link between two items.
     *
     * @param subject identifier of the subject item
     * @param object identifier of the object item
     * @return triple of ex:link between the items
     */
    private static List<RdfTerm> link(int subject, int object)
    {
        return List.of(item(subject), new Iri("http://example.org/link"), item(object));
    }


    /**
     * Triple of ex:ref from an item to a thing of the class {@code other}.
     *
     * @param subject identifier of the subject item
     * @param object identifier of the object thing
     * @return triple of ex:ref from the item to the thing
     */
    private static List<RdfTerm> ref(int subject, int object)
    {
        return List.of(item(subject), new Iri("http://example.org/ref"), new Iri("http://example.org/other/" + object));
    }


    /**
     * IRI of an item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    @Test
    @DisplayName("a variable the solutions may leave unbound")
    void optionalVariable() throws Exception
    {
        assertThat(describe("DESCRIBE ?s WHERE { OPTIONAL { ?s ex:link ?z } }"),
                is(Set.of(link(1, 2), ref(1, 7), link(2, 3))));

        assertThat(describe("DESCRIBE ?s WHERE { VALUES ?x { item:1 item:5 } OPTIONAL { ?x ex:link ?s } }"),
                is(Set.of(link(1, 2), link(2, 3))));

        assertThat(describe("DESCRIBE item:3 ?s WHERE { OPTIONAL { ?s ex:ref ?z } }"),
                is(Set.of(link(2, 3), link(1, 2), ref(1, 7))));
    }


    @Test
    @DisplayName("a variable of another class than the subjects of the triples")
    void otherClass() throws Exception
    {
        assertThat(describe("DESCRIBE ?o WHERE { OPTIONAL { ?s ex:ref ?o } }"), is(Set.of(ref(1, 7))));
    }


    @Test
    @DisplayName("a variable the solutions never bind")
    void unboundVariable() throws Exception
    {
        assertThat(describe("DESCRIBE ?s WHERE { BIND(1/0 AS ?s) }"), empty());

        assertThat(describe("DESCRIBE ?s WHERE { OPTIONAL { ?s ex:link ?z } FILTER(!BOUND(?s)) }"), empty());
    }
}
