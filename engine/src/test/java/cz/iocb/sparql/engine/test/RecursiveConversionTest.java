package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Property paths {@code *} between variables whose end variable takes a wider class than the recursive step delivers.
 * The zero-length paths enumerate every node of the graph, among them the objects of a column holding whole IRIs, so
 * the end variable gets the general IRI class, and the recursive step has to convert the node it delivers to that class
 * inside the recursive term, where its columns are addressed through its alias.
 */
public class RecursiveConversionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The node outside the class of the nodes, held as a whole IRI.
     */
    private static final Iri thing = new Iri("http://other.org/thing");

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code recursive_conversion_test} schema with the edges {@code ex:next} between the nodes named by a
     * single letter and the links {@code ex:link} from the nodes to whole IRIs, and the configuration mapping them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema recursive_conversion_test");
            statement.execute("create table recursive_conversion_test.edges (subject varchar not null, "
                    + "object varchar not null)");
            statement.execute("insert into recursive_conversion_test.edges values ('a', 'b'), ('b', 'c'), ('x', 'y')");
            statement.execute("create table recursive_conversion_test.links (subject varchar not null, "
                    + "object varchar not null)");
            statement.execute("insert into recursive_conversion_test.links values ('c', 'http://other.org/thing')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new StringUserIriClass("node", "http://example.org/node/", "[a-z]"));

        DatabaseTable edges = new DatabaseTable("recursive_conversion_test", "edges");
        DatabaseTable links = new DatabaseTable("recursive_conversion_test", "links");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(edges, graph, config.createIriMapping("node", "subject"),
                config.createIriMapping("ex:next"), config.createIriMapping("node", "object"));

        config.addQuadMapping(links, graph, config.createIriMapping("node", "subject"),
                config.createIriMapping("ex:link"), config.createIriMapping(BuiltinClasses.iri, "object"));
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Returns the SQL code the query is translated to.
     */
    private static String translate(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the node with the given name.
     */
    private static Iri node(String name)
    {
        return new Iri("http://example.org/node/" + name);
    }


    @Test
    @DisplayName("a path whose step delivers a narrower class than the zero-length paths")
    void pathOverNarrowerStep() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:next* ?y }";

        assertThat(execute(query),
                containsInAnyOrder(row(node("a"), node("a")), row(node("b"), node("b")), row(node("c"), node("c")),
                        row(node("x"), node("x")), row(node("y"), node("y")), row(thing, thing),
                        row(node("a"), node("b")), row(node("b"), node("c")), row(node("a"), node("c")),
                        row(node("x"), node("y"))));
        assertThat(translate(query), containsString("\"tab1\".\"object\""));
    }


    @Test
    @DisplayName("a path whose step delivers the class of the zero-length paths")
    void pathOverGeneralStep() throws Exception
    {
        assertThat(execute("SELECT ?x ?y WHERE { ?x ex:link* ?y }"),
                containsInAnyOrder(row(node("a"), node("a")), row(node("b"), node("b")), row(node("c"), node("c")),
                        row(node("x"), node("x")), row(node("y"), node("y")), row(thing, thing),
                        row(node("c"), thing)));
    }
}
