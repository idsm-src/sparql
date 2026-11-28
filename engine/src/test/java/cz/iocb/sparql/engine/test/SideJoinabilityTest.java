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
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
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
 * OPTIONALs whose left side is a join of children restricted against each other: the subjects of a sub-select, which
 * may be of either IRI class and which the LIMIT keeps the sub-select from leaving out, joined with the subjects of
 * {@code ex:r}, all of class {@code b}, so that the sub-select keeps the class {@code a} without columns. Whether the
 * right side can ever join the left one is decided over the components of both sides together: the sub-select alone,
 * paired with the subjects of {@code ex:q} of class {@code a}, would be compared in the class it holds without columns.
 */
public class SideJoinabilityTest
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
     * Creates the {@code side_joinability_test} schema, whose tables are mapped with the IRI classes {@code a}
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
            statement.execute("create schema side_joinability_test");
            statement.execute("create table side_joinability_test.ab (s int not null, o int not null)");
            statement.execute("insert into side_joinability_test.ab values (1, 10), (2, 20)");
            statement.execute("create table side_joinability_test.astring (s int not null, v varchar not null)");
            statement.execute("insert into side_joinability_test.astring values (1, 'x')");
            statement.execute("create table side_joinability_test.ba (s int not null, o int not null)");
            statement.execute("insert into side_joinability_test.ba values (10, 1)");
            statement.execute("create table side_joinability_test.bstring (s int not null, v varchar not null)");
            statement.execute("insert into side_joinability_test.bstring values (10, 'w')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setEngineSettings(optimized.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));
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

        configuration.addQuadMapping(new DatabaseTable("side_joinability_test", "ab"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:p"),
                configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(new DatabaseTable("side_joinability_test", "astring"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createLiteralMapping(xsdString, "v"));

        configuration.addQuadMapping(new DatabaseTable("side_joinability_test", "ba"), graph,
                configuration.createIriMapping("b", "s"), configuration.createIriMapping("ex:r"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(new DatabaseTable("side_joinability_test", "bstring"), graph,
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
    @DisplayName("an OPTIONAL that no solution of the join on the left side can match")
    void unmatchedOptional() throws Exception
    {
        String nested = "SELECT ?z ?w WHERE { { ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } } "
                + "OPTIONAL { ?x ex:q ?w } }";
        String flat = "SELECT ?z ?w WHERE { ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } "
                + "OPTIONAL { ?x ex:q ?w } }";

        assertThat(execute(nested), containsInAnyOrder(row(a(1), null), row(a(1), null)));
        assertThat(execute(flat), containsInAnyOrder(row(a(1), null), row(a(1), null)));
    }


    @Test
    @DisplayName("an OPTIONAL that the solutions of the join on the left side match")
    void matchedOptional() throws Exception
    {
        String query = "SELECT ?z ?w WHERE { { ?x ex:r ?z { SELECT ?x WHERE { ?x ?q2 ?y2 } LIMIT 100 } } "
                + "OPTIONAL { ?x ex:t ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), string("w")), row(a(1), string("w"))));
    }
}
