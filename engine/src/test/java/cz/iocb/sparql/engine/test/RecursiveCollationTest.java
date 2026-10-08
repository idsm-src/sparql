package cz.iocb.sparql.engine.test;

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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
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
 * Property paths {@code *} and {@code +} over character columns with the "C" collation. PostgreSQL requires each column
 * of the non-recursive term of a recursive query to have the collation of the whole query, so a constant, or a NULL
 * standing for a class the start does not take, has to get the collation of the columns the recursive step delivers; a
 * recursion over columns with the default collation, or one starting from columns of the same collation, is left as it
 * is.
 */
public class RecursiveCollationTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX node: <http://example.org/node/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code recursive_collation_test} schema with the same edges in a table with the "C" collation and in
     * a table with the default one, and the configuration mapping them as {@code ex:next} and {@code ex:link} between
     * the nodes named by a single letter, and the references {@code ex:refers} from items to the nodes.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema recursive_collation_test");
            statement.execute("create table recursive_collation_test.coded (subject varchar collate \"C\" not null, "
                    + "object varchar collate \"C\" not null)");
            statement.execute("insert into recursive_collation_test.coded values ('a', 'b'), ('b', 'c'), ('c', 'd'), "
                    + "('x', 'y')");
            statement.execute("create table recursive_collation_test.plain (subject varchar not null, "
                    + "object varchar not null)");
            statement.execute("insert into recursive_collation_test.plain values ('a', 'b'), ('b', 'c'), ('c', 'd'), "
                    + "('x', 'y')");
            statement.execute("create table recursive_collation_test.refers (item varchar not null, "
                    + "node varchar collate \"C\" not null)");
            statement.execute("insert into recursive_collation_test.refers values ('i', 'b')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new StringUserIriClass("node", "http://example.org/node/", "[a-z]"));
        config.addIriClass(new StringUserIriClass("item", "http://example.org/item/", "[a-z]"));

        DatabaseTable coded = new DatabaseTable("recursive_collation_test", "coded");
        DatabaseTable plain = new DatabaseTable("recursive_collation_test", "plain");
        DatabaseTable refers = new DatabaseTable("recursive_collation_test", "refers");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(coded, graph, config.createIriMapping("node", "subject"),
                config.createIriMapping("ex:next"), config.createIriMapping("node", "object"));

        config.addQuadMapping(plain, graph, config.createIriMapping("node", "subject"),
                config.createIriMapping("ex:link"), config.createIriMapping("node", "object"));

        config.addQuadMapping(refers, graph, config.createIriMapping("item", "item"),
                config.createIriMapping("ex:refers"), config.createIriMapping("node", "node"));
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
    @DisplayName("a path to a constant over columns with the C collation")
    void pathToConstant() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:next* node:c }";

        assertThat(execute(query), containsInAnyOrder(row(node("c")), row(node("b")), row(node("a"))));
        assertThat(translate(query), containsString("COLLATE \"C\""));
    }


    @Test
    @DisplayName("the number of the solutions of a path to a constant")
    void countPathToConstant() throws Exception
    {
        assertThat(execute("SELECT (COUNT(*) AS ?count) WHERE { ?s ex:next* node:c }"),
                contains(row(new TypedLiteral("3", xsdIntegerIri))));
    }


    @Test
    @DisplayName("a path from a constant over columns with the C collation")
    void pathFromConstant() throws Exception
    {
        assertThat(execute("SELECT ?o WHERE { node:b ex:next* ?o }"),
                containsInAnyOrder(row(node("b")), row(node("c")), row(node("d"))));
    }


    @Test
    @DisplayName("a path from a constant of another class")
    void pathFromOtherConstant() throws Exception
    {
        assertThat(execute("SELECT ?o WHERE { <http://example.org/node/zz> ex:next* ?o }"), contains(row(node("zz"))));
        assertThat(execute("SELECT ?s WHERE { ?s ex:next* <http://example.org/node/zz> }"), contains(row(node("zz"))));
    }


    @Test
    @DisplayName("a path from a constant of a class the recursive step does not deliver")
    void pathFromAnotherClass() throws Exception
    {
        String query = "SELECT ?o WHERE { <http://example.org/item/i> (ex:refers|ex:next)* ?o }";

        assertThat(execute(query), containsInAnyOrder(row(new Iri("http://example.org/item/i")), row(node("b")),
                row(node("c")), row(node("d"))));
        assertThat(translate(query), containsString("(NULL::varchar) COLLATE \"C\""));
    }


    @Test
    @DisplayName("a path over alternative columns with the C and the default collation")
    void pathOverAlternatives() throws Exception
    {
        assertThat(execute("SELECT DISTINCT ?o WHERE { node:b (ex:next|ex:link)* ?o }"),
                containsInAnyOrder(row(node("b")), row(node("c")), row(node("d"))));
    }


    @Test
    @DisplayName("a path over columns with the default collation is left as it is")
    void pathOverDefaultCollation() throws Exception
    {
        String query = "SELECT ?o WHERE { node:b ex:link* ?o }";

        assertThat(execute(query), containsInAnyOrder(row(node("b")), row(node("c")), row(node("d"))));
        assertThat(translate(query), not(containsString("COLLATE")));
    }


    @Test
    @DisplayName("a path whose first step has the collation of the next ones is left as it is")
    void pathFromSameCollation() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:next+ ?o }";

        assertThat(execute(query),
                containsInAnyOrder(row(node("a"), node("b")), row(node("a"), node("c")), row(node("a"), node("d")),
                        row(node("b"), node("c")), row(node("b"), node("d")), row(node("c"), node("d")),
                        row(node("x"), node("y"))));
        assertThat(translate(query), not(containsString("COLLATE")));
    }
}
