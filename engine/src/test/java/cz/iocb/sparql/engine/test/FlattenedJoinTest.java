package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Joins with a child that the optimisation turns into a join of its own children, which the parent join then holds in
 * its place (a union with a branch without solutions, an OPTIONAL whose right side has none): the children of the
 * nested join were restricted against each other, so one of them may lack the columns of a class another one excludes
 * (here the subjects of a sub-select of class {@code a} next to the subjects of {@code ex:r} of class {@code b}), and
 * the class must not be compared with the other children of the parent, neither in the joinability checks nor in the
 * join conditions, also when the join optimizer chooses the join tree.
 */
public class FlattenedJoinTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code flattened_join_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): in the default graph
     * {@code a/1 ex:p b/10}, {@code a/2 ex:p b/20} and {@code a/1 ex:q "x"}; in the graph {@code ex:g1}, which the
     * default graph includes, {@code b/10 ex:r a/1} and {@code b/10 ex:t "w"}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema flattened_join_test");
            statement.execute("create table flattened_join_test.ab (s int not null, o int not null)");
            statement.execute("insert into flattened_join_test.ab values (1, 10), (2, 20)");
            statement.execute("create table flattened_join_test.astring (s int not null, v varchar not null)");
            statement.execute("insert into flattened_join_test.astring values (1, 'x')");
            statement.execute("create table flattened_join_test.ba (s int not null, o int not null)");
            statement.execute("insert into flattened_join_test.ba values (10, 1)");
            statement.execute("create table flattened_join_test.bstring (s int not null, v varchar not null)");
            statement.execute("insert into flattened_join_test.bstring values (10, 'w')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setJoinOptimizer(true);
    }


    /**
     * Configuration mapping the test tables.
     *
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");
        configuration.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        configuration.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        ConstantIriMapping graph = configuration.createIriMapping("ex:g1");

        configuration.addQuadMapping(new DatabaseTable("flattened_join_test", "ab"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:p"),
                configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(new DatabaseTable("flattened_join_test", "astring"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createLiteralMapping(xsdString, "v"));

        configuration.addQuadMapping(new DatabaseTable("flattened_join_test", "ba"), graph,
                configuration.createIriMapping("b", "s"), configuration.createIriMapping("ex:r"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(new DatabaseTable("flattened_join_test", "bstring"), graph,
                configuration.createIriMapping("b", "s"), configuration.createIriMapping("ex:t"),
                configuration.createLiteralMapping(xsdString, "v"));

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
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
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(configuration);

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


    /**
     * IRI of the class {@code a}.
     *
     * @param id the id
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    /**
     * String literal.
     *
     * @param value the value
     * @return string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a union with a branch without solutions")
    void unionWithEmptyBranch() throws Exception
    {
        String query = "SELECT ?s1 WHERE { ?x ?p1 ?s1 . { ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } } "
                + "UNION { ?y ex:r ?z2 . ?z2 ex:t ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(1)), row(string("w")), row(string("w"))));
    }


    @Test
    @DisplayName("a union with a branch without solutions next to a sub-select")
    void unionNextToSubSelect() throws Exception
    {
        String query = "SELECT ?s1 WHERE { { SELECT ?x ?s1 WHERE { ?x ?p1 ?s1 } LIMIT 100 } "
                + "{ { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } ?x ex:r ?z } UNION { ?y ex:r ?z2 . ?z2 ex:t ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(1)), row(string("w")), row(string("w"))));
    }


    @Test
    @DisplayName("an OPTIONAL without solutions")
    void emptyOptional() throws Exception
    {
        String query = """
                SELECT ?s1 WHERE { { SELECT ?x ?s1 WHERE { ?x ?p1 ?s1 } LIMIT 100 } \
                { ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } \
                OPTIONAL { ?y ex:r ?z2 . ?z2 ex:t ?w } } }""";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(1)), row(string("w")), row(string("w"))));
    }


    @Test
    @DisplayName("EXISTS of a union with a branch without solutions")
    void exists() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER EXISTS { ?x ?p1 ?o . "
                + "{ ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } } UNION { ?y ex:r ?z2 . ?z2 ex:t ?w } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(string("w"))));
    }
}
