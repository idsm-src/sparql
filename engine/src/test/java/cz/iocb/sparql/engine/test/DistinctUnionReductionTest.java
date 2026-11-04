package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Joins of the distinct union of table accesses that the zero-length part of a property path stands for (the nodes of
 * the graph) with another access to one of the tables, which the optimisation merges into a single access: the merged
 * access has to keep the variables the other children of the join join it on, and the merge must not disturb the scan
 * over the children of the join.
 */
public class DistinctUnionReductionTest
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
     * Creates the {@code distinct_union_reduction_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): in the default graph
     * {@code a/1 ex:p b/10}, {@code a/2 ex:p b/20} and {@code a/1 ex:q "x"}; in the graph {@code ex:g1}, which the
     * default graph includes, {@code b/10 ex:r a/1}, {@code b/10 ex:t "w"}, {@code b/10 ex:u b/30} and
     * {@code b/30 ex:u b/40}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema distinct_union_reduction_test");
            statement.execute("create table distinct_union_reduction_test.ab (s int not null, o int not null)");
            statement.execute("insert into distinct_union_reduction_test.ab values (1, 10), (2, 20)");
            statement
                    .execute("create table distinct_union_reduction_test.astring (s int not null, v varchar not null)");
            statement.execute("insert into distinct_union_reduction_test.astring values (1, 'x')");
            statement.execute("create table distinct_union_reduction_test.ba (s int not null, o int not null)");
            statement.execute("insert into distinct_union_reduction_test.ba values (10, 1)");
            statement
                    .execute("create table distinct_union_reduction_test.bstring (s int not null, v varchar not null)");
            statement.execute("insert into distinct_union_reduction_test.bstring values (10, 'w')");
            statement.execute("create table distinct_union_reduction_test.bb (s int not null, o int not null)");
            statement.execute("insert into distinct_union_reduction_test.bb values (10, 30), (30, 40)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        ConstantIriMapping graph = config.createIriMapping("ex:g1");

        config.addQuadMapping(new DatabaseTable("distinct_union_reduction_test", "ab"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:p"), config.createIriMapping("b", "o"));

        config.addQuadMapping(new DatabaseTable("distinct_union_reduction_test", "astring"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:q"),
                config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("distinct_union_reduction_test", "ba"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:r"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("distinct_union_reduction_test", "bstring"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:t"),
                config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("distinct_union_reduction_test", "bb"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:u"), config.createIriMapping("b", "o"));
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
     * IRI of the class {@code b}.
     *
     * @param id the id
     * @return IRI of the class {@code b}
     */
    private static Iri b(int id)
    {
        return new Iri("http://example.org/b/" + id);
    }


    @Test
    @DisplayName("a zero-length path joined on its end")
    void joinedOnEnd() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:p ?o . ?o ex:r* ?a . ?a ex:t ?f }"), containsInAnyOrder(row(a(1))));
        assertThat(execute("SELECT DISTINCT ?x WHERE { ?x ex:p ?o . ?o ex:r* ?a . ?a ex:t ?f }"),
                containsInAnyOrder(row(a(1))));
    }


    @Test
    @DisplayName("a zero-length path joined with a GRAPH pattern")
    void joinedWithGraph() throws Exception
    {
        assertThat(execute("SELECT DISTINCT ?o WHERE { ?o ex:r* ?a . GRAPH ex:g1 { ?a ?e ?f } }"),
                containsInAnyOrder(row(b(10)), row(b(30))));
    }
}
