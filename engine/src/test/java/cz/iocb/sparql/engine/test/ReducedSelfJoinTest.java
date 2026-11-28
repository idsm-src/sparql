package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
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
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Checks that a join leaves out an access to a table whose rows another access to the table gives with the same values
 * where duplicate solutions do not matter, also when it is a branch of a union, with the join optimizer off and on, and
 * that it keeps it where the duplicates matter or the rows differ.
 *
 * The table {@code protein} holds the proteins 1 to 3; the table {@code alias}, which has no key, the aliases a, a and
 * b of the protein 1, a of the protein 2 and b of the protein 3; the table {@code alias2}, which has no key either, the
 * alias c of the protein 3. The predicate {@code ex:anyAlias} maps both alias tables.
 */
public class ReducedSelfJoinTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The configurations with the join optimizer off and on.
     */
    private static List<SparqlDatabaseConfiguration> configs = null;


    /**
     * Creates the {@code reduced_test} schema with its tables and the configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema reduced_test");
            statement.execute("create table reduced_test.protein (id int primary key, name varchar not null)");
            statement.execute("insert into reduced_test.protein values (1, 'p1'), (2, 'p2'), (3, 'p3')");
            statement.execute("create table reduced_test.alias (protein int not null, alias varchar not null)");
            statement.execute("""
                    insert into reduced_test.alias values (1, 'a'), (1, 'a'), (1, 'b'), (2, 'a'), \
                    (3, 'b')""");
            statement.execute("create table reduced_test.alias2 (protein int not null, alias varchar not null)");
            statement.execute("insert into reduced_test.alias2 values (3, 'c')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);
        configs = new ArrayList<>();

        for(boolean optimizer : new boolean[] { false, true })
        {
            SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
            config.setEngineSettings(config.getEngineSettings()
                    .withJoinOptimizer(optimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

            config.addPrefix("ex", "http://example.org/");
            config.addIriClass(new IntegerUserIriClass("protein", INT4, "http://example.org/protein/"));

            DatabaseTable protein = new DatabaseTable("reduced_test", "protein");
            DatabaseTable alias = new DatabaseTable("reduced_test", "alias");
            DatabaseTable alias2 = new DatabaseTable("reduced_test", "alias2");
            ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:name"), config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(alias, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:alias"), config.createLiteralMapping(xsdString, "alias"));
            config.addQuadMapping(alias, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:anyAlias"), config.createLiteralMapping(xsdString, "alias"));
            config.addQuadMapping(alias2, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:anyAlias"), config.createLiteralMapping(xsdString, "alias"));

            configs.add(config);
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, ServiceException
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
     * Runs the query and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query)
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
     * How many times the SQL reads the table.
     *
     * @param sql the SQL
     * @param table the name of a table of the test schema
     * @return number of the accesses to the table in the SQL
     */
    private static int reads(String sql, String table)
    {
        String name = "\"reduced_test\".\"" + table + "\"";

        return (sql.length() - sql.replace(name, "").length()) / name.length();
    }


    /**
     * Expected row of a protein and terms.
     *
     * @param protein the protein id
     * @param terms the terms, null for an unbound value
     * @return row of the protein and the terms
     */
    private static List<RdfTerm> row(int protein, RdfTerm... terms)
    {
        List<RdfTerm> row = new ArrayList<>();
        row.add(new Iri("http://example.org/protein/" + protein));
        row.addAll(Arrays.asList(terms));
        return row;
    }


    /**
     * A string literal.
     *
     * @param value the value
     * @return the literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a DISTINCT join leaves out an access whose rows another access gives")
    void distinctJoin() throws Exception
    {
        String query = "SELECT DISTINCT ?p WHERE { ?p ex:alias ?a . ?p ex:alias \"a\" }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1), row(2)));
            assertThat(reads(translate(config, query), "alias"), is(1));
        }
    }


    @Test
    @DisplayName("the pattern of an EXISTS leaves out an access whose rows another access gives")
    void existsPattern() throws Exception
    {
        String query = "SELECT ?p WHERE { ?p ex:name ?n FILTER EXISTS { ?p ex:alias ?a . ?p ex:alias \"a\" } }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1), row(2)));
            assertThat(reads(translate(config, query), "alias"), is(1));
        }
    }


    @Test
    @DisplayName("a DISTINCT join leaves out a union with a branch whose rows another access gives")
    void distinctUnion() throws Exception
    {
        String query = "SELECT DISTINCT ?p WHERE { ?p ex:anyAlias ?x . ?p ex:alias \"a\" }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1), row(2)));
            assertThat(reads(translate(config, query), "alias2"), is(0));
        }
    }


    @Test
    @DisplayName("a join keeps the multiplicities where duplicate solutions matter")
    void multiset() throws Exception
    {
        String query = "SELECT ?p ?a WHERE { ?p ex:alias ?a . ?p ex:alias \"a\" }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, string("a")), row(1, string("a")), row(1, string("a")),
                            row(1, string("a")), row(1, string("b")), row(1, string("b")), row(2, string("a"))));
    }


    @Test
    @DisplayName("a DISTINCT join keeps accesses restricted to different rows")
    void differentRows() throws Exception
    {
        String query = "SELECT DISTINCT ?p WHERE { ?p ex:alias \"a\" . ?p ex:alias \"b\" }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1)));
    }


    @Test
    @DisplayName("a DISTINCT join keeps a union binding a variable the other access does not bind")
    void unionWithOwnVariable() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?x WHERE { ?p ex:anyAlias ?x . ?p ex:alias \"a\" }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, string("a")), row(1, string("b")), row(2, string("a"))));
    }
}
